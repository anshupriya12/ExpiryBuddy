package com.expirybuddy.app.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.photo.PhotoStorage
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.expiry.ExpiryClassifier
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.domain.expiry.todayUpdates
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.ui.navigation.Destinations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data object Error : DetailsUiState

    /** The item was deleted by this screen; the UI should navigate back. */
    data object Deleted : DetailsUiState

    /** The item was marked as used by this screen; the UI should navigate back. */
    data object MarkedUsed : DetailsUiState

    /**
     * @property status expiry status for today's date (not meaningful for the user once [item] is used).
     * @property daysUntilExpiry negative when expired, 0 when it expires today.
     */
    data class Found(
        val item: ExpiryItem,
        val status: ExpiryStatus,
        val daysUntilExpiry: Long,
        val deleteFailed: Boolean = false,
        val updateFailed: Boolean = false,
    ) : DetailsUiState
}

private enum class ActionStatus { IDLE, IN_PROGRESS, DELETE_FAILED, UPDATE_FAILED, DELETED, MARKED_USED }

class ItemDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpiryItemRepository,
    private val clock: Clock,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>(Destinations.ITEM_ID_ARG)
        ?: Destinations.NO_ITEM_ID

    private val action = MutableStateFlow(ActionStatus.IDLE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailsUiState> =
        combine(repository.observeItem(itemId), action, clock.todayUpdates()) { item, action, today ->
            when {
                action == ActionStatus.DELETED -> DetailsUiState.Deleted
                action == ActionStatus.MARKED_USED -> DetailsUiState.MarkedUsed
                // While an action runs, the item changing in the database must not flash "not found".
                action == ActionStatus.IN_PROGRESS -> DetailsUiState.Loading
                item == null -> DetailsUiState.NotFound
                else -> DetailsUiState.Found(
                    item = item,
                    status = ExpiryClassifier.classify(item.expiryDate, today),
                    daysUntilExpiry = ExpiryClassifier.daysUntil(item.expiryDate, today),
                    deleteFailed = action == ActionStatus.DELETE_FAILED,
                    updateFailed = action == ActionStatus.UPDATE_FAILED,
                )
            }
        }
            .onStart { emit(DetailsUiState.Loading) }
            .catch { emit(DetailsUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    private fun currentItem(): ExpiryItem? = (uiState.value as? DetailsUiState.Found)?.item

    fun onDelete() {
        if (action.value == ActionStatus.IN_PROGRESS) return
        val photoUri = currentItem()?.photoUri
        action.value = ActionStatus.IN_PROGRESS
        viewModelScope.launch {
            try {
                // Deleting through the repository also cancels the item's reminder.
                repository.delete(itemId)
                photoStorage.delete(photoUri)
                action.value = ActionStatus.DELETED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                action.value = ActionStatus.DELETE_FAILED
            }
        }
    }

    /** Keeps the item (nothing is deleted), takes it off the active lists and cancels its reminder. */
    fun onMarkUsed() = setUsed(used = true)

    /** Puts a used item back on the active lists; saving re-plans its reminder if one still applies. */
    fun onRestore() = setUsed(used = false)

    private fun setUsed(used: Boolean) {
        if (action.value == ActionStatus.IN_PROGRESS) return
        val item = currentItem() ?: return
        if (item.isUsed == used) return
        action.value = ActionStatus.IN_PROGRESS
        viewModelScope.launch {
            try {
                val now = Instant.now(clock)
                repository.save(item.copy(usedAt = if (used) now else null, updatedAt = now))
                action.value = if (used) ActionStatus.MARKED_USED else ActionStatus.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                action.value = ActionStatus.UPDATE_FAILED
            }
        }
    }
}
