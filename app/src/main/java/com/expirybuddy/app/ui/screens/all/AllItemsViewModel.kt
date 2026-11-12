package com.expirybuddy.app.ui.screens.all

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.expiry.ClassifiedItem
import com.expirybuddy.app.domain.expiry.InventoryBuilder
import com.expirybuddy.app.domain.expiry.ItemFilter
import com.expirybuddy.app.domain.expiry.ItemSort
import com.expirybuddy.app.domain.expiry.todayUpdates
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock

sealed interface AllItemsUiState {
    data object Loading : AllItemsUiState
    data object Error : AllItemsUiState

    /**
     * @property items the visible rows for the current mode, filter and sort.
     * @property modeTotal how many items exist in the current mode before filtering. 0 means "nothing
     * here yet", as opposed to "nothing matches the filter".
     * @property showUsed true while the list shows items marked as used instead of active ones.
     * @property hasUsedItems whether the "Used" filter has anything behind it.
     */
    data class Content(
        val items: List<ClassifiedItem>,
        val filter: ItemFilter,
        val sort: ItemSort,
        val showUsed: Boolean,
        val modeTotal: Int,
        val hasUsedItems: Boolean,
    ) : AllItemsUiState
}

/**
 * The full inventory: search, category filter and sort, reusing [ItemFilter] and [InventoryBuilder].
 * Filter, sort and mode live in [SavedStateHandle] so they survive rotation and process death.
 */
class AllItemsViewModel(
    private val savedStateHandle: SavedStateHandle,
    repository: ExpiryItemRepository,
    private val clock: Clock,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val categoryKey = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val sortKey = savedStateHandle.getStateFlow(KEY_SORT, ItemSort.EXPIRY_DATE.name)
    private val showUsed = savedStateHandle.getStateFlow(KEY_SHOW_USED, false)

    private val reloadTrigger = MutableStateFlow(0)
    private val clockTick = MutableStateFlow(0)

    private data class Controls(val filter: ItemFilter, val sort: ItemSort, val showUsed: Boolean)

    private val controls = combine(query, categoryKey, sortKey, showUsed) { query, category, sort, used ->
        Controls(
            filter = ItemFilter(query, category?.let(ItemCategory::fromStorageKey)),
            sort = ItemSort.entries.firstOrNull { it.name == sort } ?: ItemSort.EXPIRY_DATE,
            showUsed = used,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AllItemsUiState> = reloadTrigger
        .flatMapLatest {
            combine(
                repository.observeItems(),
                controls,
                clockTick.flatMapLatest { clock.todayUpdates() },
            ) { items, controls, today ->
                buildContent(items, controls, today)
            }
                .onStart { emit(AllItemsUiState.Loading) }
                .catch { emit(AllItemsUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllItemsUiState.Loading)

    private fun buildContent(items: List<ExpiryItem>, controls: Controls, today: java.time.LocalDate): AllItemsUiState {
        val usedCount = items.count { it.isUsed }
        val visible = if (controls.showUsed) {
            InventoryBuilder.used(items, controls.filter, today)
        } else {
            InventoryBuilder.active(items, controls.filter, controls.sort, today)
        }
        return AllItemsUiState.Content(
            items = visible,
            filter = controls.filter,
            sort = controls.sort,
            showUsed = controls.showUsed,
            modeTotal = if (controls.showUsed) usedCount else items.size - usedCount,
            hasUsedItems = usedCount > 0,
        )
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(MAX_QUERY_LENGTH)
    }

    fun onCategorySelected(category: ItemCategory?) {
        savedStateHandle[KEY_CATEGORY] = category?.storageKey
    }

    fun onSortSelected(sort: ItemSort) {
        savedStateHandle[KEY_SORT] = sort.name
    }

    fun onShowUsedChange(showUsed: Boolean) {
        savedStateHandle[KEY_SHOW_USED] = showUsed
    }

    fun onClearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_CATEGORY] = null
    }

    /** Re-reads today's date (and restarts the midnight timer); call when the screen resumes. */
    fun onResumed() {
        clockTick.update { it + 1 }
    }

    fun retry() {
        reloadTrigger.update { it + 1 }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_SORT = "sort"
        const val KEY_SHOW_USED = "showUsed"
        const val MAX_QUERY_LENGTH = 100
    }
}
