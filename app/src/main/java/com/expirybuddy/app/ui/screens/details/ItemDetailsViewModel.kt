package com.expirybuddy.app.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.ui.navigation.Destinations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data object Error : DetailsUiState

    /** The item was deleted by this screen; the UI should navigate back. */
    data object Deleted : DetailsUiState

    data class Found(val item: ExpiryItem, val deleteFailed: Boolean = false) : DetailsUiState
}

private enum class DeleteStatus { IDLE, IN_PROGRESS, FAILED, DONE }

class ItemDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpiryItemRepository,
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>(Destinations.ITEM_ID_ARG)
        ?: Destinations.NO_ITEM_ID

    private val deleteStatus = MutableStateFlow(DeleteStatus.IDLE)

    val uiState: StateFlow<DetailsUiState> =
        combine(repository.observeItem(itemId), deleteStatus) { item, delete ->
            when {
                delete == DeleteStatus.DONE -> DetailsUiState.Deleted
                // While deleting, the item vanishing from the database must not flash "not found".
                delete == DeleteStatus.IN_PROGRESS -> DetailsUiState.Loading
                item == null -> DetailsUiState.NotFound
                else -> DetailsUiState.Found(item, deleteFailed = delete == DeleteStatus.FAILED)
            }
        }
            .onStart { emit(DetailsUiState.Loading) }
            .catch { emit(DetailsUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    fun onDelete() {
        if (deleteStatus.value == DeleteStatus.IN_PROGRESS) return
        deleteStatus.value = DeleteStatus.IN_PROGRESS
        viewModelScope.launch {
            try {
                repository.delete(itemId)
                deleteStatus.value = DeleteStatus.DONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deleteStatus.value = DeleteStatus.FAILED
            }
        }
    }
}
