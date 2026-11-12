package com.expirybuddy.app.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.photo.PhotoStorage
import com.expirybuddy.app.data.prefs.UserPreferences
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.validation.DateError
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
    val expiryDate: LocalDate? = null,
    val notes: String = "",
    val reminderDaysBefore: Int? = null,
    val photoUri: String? = null,
    /** A picture is being copied into private storage. */
    val isPhotoBusy: Boolean = false,
    /** The last picture could not be read; the previous photo (if any) was kept. */
    val photoFailed: Boolean = false,
    val nameError: NameError? = null,
    val dateError: DateError? = null,
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
                            expiryDate = item.expiryDate,
                            notes = item.notes.orEmpty(),
                            reminderDaysBefore = item.reminderDaysBefore,
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

    fun onExpiryDateChange(value: LocalDate) = _uiState.update {
        it.copy(expiryDate = value, dateError = null)
    }

    fun onNotesChange(value: String) = _uiState.update {
        it.copy(notes = value.take(ItemFormValidator.MAX_NOTES_LENGTH))
    }

    fun onReminderChange(daysBefore: Int?) = _uiState.update { it.copy(reminderDaysBefore = daysBefore) }

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

        val validation = ItemFormValidator.validate(current.name, current.expiryDate)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(nameError = validation.nameError, dateError = validation.dateError, saveFailed = false)
            }
            return
        }

        if (canExplainNotifications && current.reminderDaysBefore != null) {
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

    private fun save(current: AddEditUiState) {
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        val now = Instant.now(clock)
        val item = ExpiryItem(
            id = original?.id ?: ExpiryItem.NEW_ID,
            name = current.name.trim(),
            category = current.category,
            expiryDate = checkNotNull(current.expiryDate),
            notes = current.notes.trim().ifBlank { null },
            reminderDaysBefore = current.reminderDaysBefore,
            createdAt = original?.createdAt ?: now,
            photoUri = current.photoUri,
            updatedAt = now,
            usedAt = original?.usedAt,
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
