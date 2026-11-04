package com.expirybuddy.app.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.expiry.ItemFilter
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.MainDispatcherRule
import com.expirybuddy.app.testing.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HomeViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-03-15T12:00:00Z"))
    private val today = LocalDate.of(2026, 3, 15)

    private fun item(
        id: Long,
        name: String,
        daysFromToday: Long,
        category: ItemCategory = ItemCategory.FOOD,
    ) = ExpiryItem(
        id = id,
        name = name,
        category = category,
        expiryDate = today.plusDays(daysFromToday),
        createdAt = Instant.EPOCH,
    )

    private val sample = listOf(
        item(1, "old milk", -3),
        item(2, "eggs", 2),
        item(3, "passport", 300, ItemCategory.DOCUMENT),
    )

    private fun viewModel(
        repo: FakeExpiryItemRepository = FakeExpiryItemRepository(sample),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = HomeViewModel(handle, repo, clock)

    private suspend fun HomeViewModel.content(
        predicate: (HomeUiState.Content) -> Boolean = { true },
    ): HomeUiState.Content = uiState.first { it is HomeUiState.Content && predicate(it) } as HomeUiState.Content

    @Test
    fun emptyRepository_reportsZeroTotal() = runTest {
        val content = viewModel(FakeExpiryItemRepository()).content()

        assertEquals(0, content.totalCount)
        assertTrue(content.sections.isEmpty)
    }

    @Test
    fun items_areGroupedIntoStatusSections() = runTest {
        val content = viewModel().content()

        assertEquals(3, content.totalCount)
        assertEquals(listOf("old milk"), content.sections.expired.map { it.item.name })
        assertEquals(listOf("eggs"), content.sections.expiringSoon.map { it.item.name })
        assertEquals(listOf("passport"), content.sections.upcoming.map { it.item.name })
    }

    @Test
    fun query_filtersListButKeepsCounts() = runTest {
        val vm = viewModel()
        vm.onQueryChange("eggs")

        val content = vm.content { it.filter.query == "eggs" }

        assertEquals(1, content.sections.visibleCount)
        assertEquals(3, content.sections.counts.total)
        assertEquals("eggs", vm.query.value)
    }

    @Test
    fun category_filtersList() = runTest {
        val vm = viewModel()
        vm.onCategorySelected(ItemCategory.DOCUMENT)

        val content = vm.content { it.filter.category == ItemCategory.DOCUMENT }

        assertEquals(listOf("passport"), content.sections.upcoming.map { it.item.name })
        assertTrue(content.sections.expired.isEmpty())
    }

    @Test
    fun filterWithNoMatches_isEmptyButTotalStaysNonZero() = runTest {
        val vm = viewModel()
        vm.onQueryChange("zzz")

        val content = vm.content { it.filter.query == "zzz" }

        assertTrue(content.sections.isEmpty)
        assertEquals(3, content.totalCount)
    }

    @Test
    fun clearFilters_resetsQueryAndCategory() = runTest {
        val vm = viewModel()
        vm.onQueryChange("eggs")
        vm.onCategorySelected(ItemCategory.FOOD)

        vm.onClearFilters()

        val content = vm.content { !it.filter.isActive }
        assertEquals(ItemFilter(), content.filter)
        assertEquals("", vm.query.value)
        assertEquals(3, content.sections.visibleCount)
    }

    @Test
    fun filterState_isRestoredFromSavedStateHandle() = runTest {
        val handle = SavedStateHandle(mapOf("query" to "milk", "category" to "food"))

        val content = viewModel(handle = handle).content()

        assertEquals(ItemFilter("milk", ItemCategory.FOOD), content.filter)
        assertEquals(listOf("old milk"), content.sections.expired.map { it.item.name })
    }

    @Test
    fun overlongQuery_isCapped() = runTest {
        val vm = viewModel()
        vm.onQueryChange("x".repeat(500))
        assertEquals(100, vm.query.value.length)
    }

    @Test
    fun onResumed_reclassifiesWhenTheDateHasChanged() = runTest {
        val vm = viewModel()
        assertEquals(1, vm.content().sections.counts.expiringSoon)

        // Two days pass while the app sits in the background: "eggs" (due in 2 days) is now due today
        // and "old milk" is five days overdue.
        clock.now = Instant.parse("2026-03-17T12:00:00Z")
        vm.onResumed()

        val content = vm.content { it.sections.expired.size == 1 && it.sections.expired.first().daysUntilExpiry == -5L }
        assertEquals(-5L, content.sections.expired.first().daysUntilExpiry)
        assertEquals(0L, content.sections.expiringSoon.first().daysUntilExpiry)
    }

    @Test
    fun loadFailure_yieldsError_andRetryRecovers() = runTest {
        val repo = FakeExpiryItemRepository(sample)
        repo.failObserve = true
        val vm = viewModel(repo)

        assertEquals(HomeUiState.Error, vm.uiState.first { it !is HomeUiState.Loading })

        repo.failObserve = false
        vm.retry()

        assertEquals(3, vm.content().totalCount)
    }
}
