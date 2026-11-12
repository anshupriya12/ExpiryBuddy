package com.expirybuddy.app.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.photo.PhotoStorage
import com.expirybuddy.app.data.prefs.UserPreferences
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.expiry.UseByCalculator
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import com.expirybuddy.app.domain.model.ReminderOptions
import com.expirybuddy.app.domain.model.StorageType
import com.expirybuddy.app.domain.validation.CustomReminderError
import com.expirybuddy.app.domain.validation.DateError
import com.expirybuddy.app.domain.validation.UseWithinError
import com.expirybuddy.app.domain.validation.ItemFormValidator
import com.expirybuddy.app.domain.validation.NameError
import com.expirybuddy.app.ui.navigation.Destinations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

enum class LoadFailure { NOT_FOUND, ERROR }

data class AddEditUiState(
    val isEditing: Boolean,
    val isLoading: Boolean,
    val loadFailure: LoadFailure? = null,
    val name: String = "",
    val category: ItemCategory = ItemCategory.FOOD,
    /** The use-by date: the one deadline. Typed by the user or calculated from [openedOn] + use within. */
    val useByDate: LocalDate? = null,
    val openedOn: LocalDate? = null,
    /** Text of the "use within" number, kept as typed so validation can explain a bad value. */
    val useWithinText: String = "",
    val useWithinUnit: UseWithinUnit = UseWithinUnit.DAYS,
    /** True while [useByDate] is the calculated one, so the form can say where it came from. */
    val useByCalculated: Boolean = false,
    /** Optional quick storage choice; null = not specified. */
    val storageType: StorageType? = null,
    /** Multiline "additional instructions". */
    val storageInstructions: String = "",
    val notes: String = "",
    /** Days before the use-by date to remind; null = no reminder. Ignored while [reminderIsCustom]. */
    val reminderDaysBefore: Int? = null,
    /** The user chose "Custom": the number of days is in [customReminderText]. */
    val reminderIsCustom: Boolean = false,
    val customReminderText: String = "",
    /** Item-specific reminder time in minutes after midnight; null = follow the default time. */
    val reminderMinuteOfDay: Int? = null,
    /** Default notification time from Settings, shown for items that follow it. */
    val defaultReminderMinutes: Int = 540,
    val photoUri: String? = null,
    /** A picture is being copied into private storage. */
    val isPhotoBusy: Boolean = false,
    /** The last picture could not be read; the previous photo (if any) was kept. */
    val photoFailed: Boolean = false,
    val nameError: NameError? = null,
    val dateError: DateError? = null,
    val useWithinError: UseWithinError? = null,
    val customReminderError: CustomReminderError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
    /** The item was deleted from the edit screen; the UI should leave, as there is nothing left to show. */
    val deleted: Boolean = false,
    /** The form is valid and a reminder is chosen: explain notifications before saving. */
    val showNotificationExplanation: Boolean = false,
)

class AddEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpiryItemRepository,
    private val clock: Clock,
    private val photoStorage: PhotoStorage,
    preferences: UserPreferences,
) : ViewModel() {

    private val itemId: Long? = savedStateHandle.get<Long>(Destinations.ITEM_ID_ARG)
        ?.takeIf { it != Destinations.NO_ITEM_ID }

    /** The item being edited, kept so a save preserves its id, creation time and used state. */
    private var original: ExpiryItem? = null

    private val _uiState = MutableStateFlow(
        AddEditUiState(
            isEditing = itemId != null,
            isLoading = itemId != null,
            // New items start with the default reminder chosen in Settings.
            reminderDaysBefore = preferences.defaultReminderDays,
            reminderIsCustom = preferences.defaultReminderDays?.let { !ReminderOptions.isPreset(it) } ?: false,
            customReminderText = preferences.defaultReminderDays
                ?.takeIf { !ReminderOptions.isPreset(it) }?.toString().orEmpty(),
            defaultReminderMinutes = preferences.defaultReminderMinutes,
        ),
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (itemId != null) loadItem(itemId)
    }

    private fun loadItem(id: Long) {
        viewModelScope.launch {
            try {
                val item = repository.getItem(id)
                if (item == null) {
                    _uiState.update { it.copy(isLoading = false, loadFailure = LoadFailure.NOT_FOUND) }
                } else {
                    original = item
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = item.name,
                            category = item.category,
                            useByDate = item.expiryDate,
                            openedOn = item.openedOn,
                            useWithinText = item.useWithinAmount?.toString().orEmpty(),
                            useWithinUnit = item.useWithinUnit,
                            storageType = item.storageType,
                            storageInstructions = item.storageInstructions.orEmpty(),
                            notes = item.notes.orEmpty(),
                            reminderDaysBefore = item.reminderDaysBefore,
                            reminderIsCustom = item.reminderDaysBefore?.let { d -> !ReminderOptions.isPreset(d) } ?: false,
                            customReminderText = item.reminderDaysBefore
                                ?.takeIf { d -> !ReminderOptions.isPreset(d) }?.toString().orEmpty(),
                            reminderMinuteOfDay = item.reminderMinuteOfDay,
                            photoUri = item.photoUri,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailure = LoadFailure.ERROR) }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value.take(ItemFormValidator.MAX_NAME_LENGTH), nameError = null)
    }

    fun onCategoryChange(value: ItemCategory) = _uiState.update { it.copy(category = value) }

    /** The user picked the use-by date by hand; it stays as chosen until the opening details change again. */
    fun onUseByDateChange(value: LocalDate) = _uiState.update {
        it.copy(useByDate = value, useByCalculated = false, dateError = null)
    }

    fun onOpenedOnChange(value: LocalDate?) = _uiState.update { recalculate(it.copy(openedOn = value)) }

    fun onUseWithinChange(text: String) = _uiState.update {
        recalculate(it.copy(useWithinText = text.filter(Char::isDigit).take(4), useWithinError = null))
    }

    fun onUseWithinUnitChange(unit: UseWithinUnit) = _uiState.update {
        recalculate(it.copy(useWithinUnit = unit, useWithinError = null))
    }

    /** use-by = opened on + use within, whenever both are valid. Otherwise the date is left alone. */
    private fun recalculate(state: AddEditUiState): AddEditUiState {
        val calculated = UseByCalculator.calculate(
            state.openedOn,
            state.useWithinText.trim().toIntOrNull(),
            state.useWithinUnit,
        ) ?: return state
        return state.copy(useByDate = calculated, useByCalculated = true, dateError = null)
    }

    fun onStorageTypeChange(value: StorageType?) = _uiState.update { it.copy(storageType = value) }

    fun onStorageChange(value: String) = _uiState.update {
        it.copy(storageInstructions = value.take(ItemFormValidator.MAX_STORAGE_LENGTH))
    }

    fun onNotesChange(value: String) = _uiState.update {
        it.copy(notes = value.take(ItemFormValidator.MAX_NOTES_LENGTH))
    }

    /** A preset was chosen: no reminder, the use-by day, or 1 to 3 days before. */
    fun onReminderChange(daysBefore: Int?) = _uiState.update {
        it.copy(reminderDaysBefore = daysBefore, reminderIsCustom = false, customReminderError = null)
    }

    /** "Custom" was chosen: the number of days is typed in. */
    fun onCustomReminderSelected() = _uiState.update {
        it.copy(
            reminderIsCustom = true,
            customReminderText = it.customReminderText.ifBlank {
                it.reminderDaysBefore?.takeIf { d -> !ReminderOptions.isPreset(d) }?.toString().orEmpty()
            },
            customReminderError = null,
        )
    }

    fun onCustomReminderTextChange(text: String) = _uiState.update {
        val digits = text.filter(Char::isDigit).take(3)
        it.copy(
            customReminderText = digits,
            reminderDaysBefore = digits.toIntOrNull()?.takeIf(ReminderOptions::isValidCustom) ?: it.reminderDaysBefore,
            customReminderError = null,
        )
    }

    /** A time for this item only; null goes back to the default notification time. */
    fun onReminderTimeChange(minuteOfDay: Int?) = _uiState.update { it.copy(reminderMinuteOfDay = minuteOfDay) }

    /**
     * A picture was chosen from the gallery or taken with the camera. It is copied into private
     * storage straight away; a photo that replaces an unsaved one deletes the unsaved one.
     */
    fun onPhotoPicked(sourceUri: String) {
        if (_uiState.value.isPhotoBusy) return
        _uiState.update { it.copy(isPhotoBusy = true, photoFailed = false) }
        viewModelScope.launch {
            val stored = photoStorage.importPhoto(sourceUri)
            _uiState.update { current ->
                if (stored == null) {
                    current.copy(isPhotoBusy = false, photoFailed = true)
                } else {
                    discardIfUnsaved(current.photoUri)
                    current.copy(isPhotoBusy = false, photoUri = stored)
                }
            }
        }
    }

    fun onPhotoRemoved() = _uiState.update { current ->
        discardIfUnsaved(current.photoUri)
        current.copy(photoUri = null, photoFailed = false)
    }

    /** A photo that is not the saved item's own photo exists only for this form, so it can go. */
    private fun discardIfUnsaved(photoUri: String?) {
        if (photoUri != null && photoUri != original?.photoUri) photoStorage.delete(photoUri)
    }

    /**
     * @param canExplainNotifications true when the screen would like to explain notification
     * permission first (Android 13+, permission missing, never explained). The explanation is shown
     * only for a valid form with a reminder chosen, and the item is saved either way afterwards.
     */
    fun onSave(canExplainNotifications: Boolean = false) {
        val current = _uiState.value
        if (current.isSaving || current.isLoading || current.loadFailure != null || current.isPhotoBusy) return
        if (current.showNotificationExplanation) return

        val validation = ItemFormValidator.validate(
            name = current.name,
            useByDate = current.useByDate,
            useWithinText = current.useWithinText,
            useWithinUnit = current.useWithinUnit,
            customReminderText = current.customReminderText.takeIf { current.reminderIsCustom },
        )
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    nameError = validation.nameError,
                    dateError = validation.dateError,
                    useWithinError = validation.useWithinError,
                    customReminderError = validation.customReminderError,
                    saveFailed = false,
                )
            }
            return
        }

        if (canExplainNotifications && effectiveReminderDays(current) != null) {
            _uiState.update { it.copy(showNotificationExplanation = true, saveFailed = false) }
            return
        }
        save(current)
    }

    /** Called when the user has dealt with the notification explanation/permission (allowed or not). */
    fun onNotificationExplanationHandled() {
        val current = _uiState.value
        if (!current.showNotificationExplanation) return
        _uiState.update { it.copy(showNotificationExplanation = false) }
        save(current)
    }

    /** The number of days that will be saved: the custom number if "Custom" is chosen, else the preset. */
    private fun effectiveReminderDays(state: AddEditUiState): Int? =
        if (state.reminderIsCustom) state.customReminderText.trim().toIntOrNull() else state.reminderDaysBefore

    private fun save(current: AddEditUiState) {
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        val now = Instant.now(clock)
        val item = ExpiryItem(
            id = original?.id ?: ExpiryItem.NEW_ID,
            name = current.name.trim(),
            category = current.category,
            expiryDate = checkNotNull(current.useByDate),
            notes = current.notes.trim().ifBlank { null },
            reminderDaysBefore = effectiveReminderDays(current),
            createdAt = original?.createdAt ?: now,
            photoUri = current.photoUri,
            updatedAt = now,
            usedAt = original?.usedAt,
            openedOn = current.openedOn,
            useWithinAmount = current.useWithinText.trim().toIntOrNull(),
            useWithinUnit = current.useWithinUnit,
            storageType = current.storageType,
            storageInstructions = current.storageInstructions.trim().ifBlank { null },
            reminderMinuteOfDay = current.reminderMinuteOfDay,
        )
        viewModelScope.launch {
            try {
                repository.save(item)
                // The old photo is only deleted once the new data is safely saved.
                original?.photoUri?.takeIf { it != item.photoUri }?.let(photoStorage::delete)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /** Edit mode only: deletes the item (the repository also cancels its reminder) and its photo. */
    fun onDelete() {
        val item = original ?: return
        val current = _uiState.value
        if (current.isDeleting || current.isSaving || current.isLoading) return
        _uiState.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            try {
                repository.delete(item.id)
                photoStorage.delete(item.photoUri)
                discardIfUnsaved(_uiState.value.photoUri)
                _uiState.update { it.copy(isDeleting = false, deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }

    /** Leaving without saving: drops a picture that was only ever attached to this unsaved form. */
    internal fun discardUnsavedPhoto() {
        val state = _uiState.value
        if (!state.saved && !state.deleted) discardIfUnsaved(state.photoUri)
    }

    override fun onCleared() = discardUnsavedPhoto()
}
