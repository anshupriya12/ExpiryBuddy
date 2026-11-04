package com.expirybuddy.app.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.model.ReminderOptions
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
    val reminderDaysBefore: Int? = ReminderOptions.DEFAULT_DAYS_BEFORE,
    val nameError: NameError? = null,
    val dateError: DateError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    /** The form is valid and a reminder is chosen: explain notifications before saving. */
    val showNotificationExplanation: Boolean = false,
)

class AddEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpiryItemRepository,
    private val clock: Clock,
) : ViewModel() {

    private val itemId: Long? = savedStateHandle.get<Long>(Destinations.ITEM_ID_ARG)
        ?.takeIf { it != Destinations.NO_ITEM_ID }

    /** The item being edited, kept so a save preserves its id and creation time. */
    private var original: ExpiryItem? = null

    private val _uiState = MutableStateFlow(
        AddEditUiState(isEditing = itemId != null, isLoading = itemId != null),
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
     * @param canExplainNotifications true when the screen would like to explain notification
     * permission first (Android 13+, permission missing, never explained). The explanation is shown
     * only for a valid form with a reminder chosen, and the item is saved either way afterwards.
     */
    fun onSave(canExplainNotifications: Boolean = false) {
        val current = _uiState.value
        if (current.isSaving || current.isLoading || current.loadFailure != null) return
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
        val item = ExpiryItem(
            id = original?.id ?: ExpiryItem.NEW_ID,
            name = current.name.trim(),
            category = current.category,
            expiryDate = checkNotNull(current.expiryDate),
            notes = current.notes.trim().ifBlank { null },
            reminderDaysBefore = current.reminderDaysBefore,
            createdAt = original?.createdAt ?: Instant.now(clock),
        )
        viewModelScope.launch {
            try {
                repository.save(item)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
