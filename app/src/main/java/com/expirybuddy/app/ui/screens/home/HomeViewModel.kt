package com.expirybuddy.app.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.domain.expiry.HomeSections
import com.expirybuddy.app.domain.expiry.HomeSectionsBuilder
import com.expirybuddy.app.domain.expiry.ItemFilter
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
import java.time.LocalDate

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState

    /**
     * @property totalCount all saved items, ignoring the filter. 0 means show the first-run empty state.
     * @property sections grouped, sorted, filtered items plus unfiltered status counts.
     */
    data class Content(
        val totalCount: Int,
        val sections: HomeSections,
        val filter: ItemFilter,
    ) : HomeUiState
}

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    repository: ExpiryItemRepository,
    private val clock: Clock,
) : ViewModel() {

    /**
     * Search text, exposed separately so the text field reads it synchronously. Filter state lives
     * in [SavedStateHandle] so it survives the app being killed in the background.
     */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val categoryKey: StateFlow<String?> = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null)

    private val reloadTrigger = MutableStateFlow(0)
    private val clockTick = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = reloadTrigger
        .flatMapLatest {
            combine(
                repository.observeItems(),
                query,
                categoryKey,
                clockTick.flatMapLatest { clock.todayUpdates() },
            ) { items, query, categoryKey, today ->
                val filter = ItemFilter(query, categoryKey?.let(ItemCategory::fromStorageKey))
                buildContent(items, filter, today)
            }
                .onStart { emit(HomeUiState.Loading) }
                .catch { emit(HomeUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private fun buildContent(items: List<ExpiryItem>, filter: ItemFilter, today: LocalDate): HomeUiState =
        HomeUiState.Content(
            totalCount = items.size,
            sections = HomeSectionsBuilder.build(items, filter, today),
            filter = filter,
        )

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(MAX_QUERY_LENGTH)
    }

    fun onCategorySelected(category: ItemCategory?) {
        savedStateHandle[KEY_CATEGORY] = category?.storageKey
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
        const val MAX_QUERY_LENGTH = 100
    }
}
