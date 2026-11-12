package com.expirybuddy.app.ui.screens.all

import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.expiry.ItemSort
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.MainDispatcherRule
import com.expirybuddy.app.testing.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AllItemsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-03-15T12:00:00Z"))
    private val today = LocalDate.of(2026, 3, 15)

    private fun item(
        id: Long,
        name: String,
        daysFromToday: Long,
        category: ItemCategory = ItemCategory.FOOD,
        usedAt: Instant? = null,
    ) = ExpiryItem(
        id = id,
        name = name,
        category = category,
        expiryDate = today.plusDays(daysFromToday),
        createdAt = Instant.EPOCH,
        usedAt = usedAt,
    )

    private val sample = listOf(
        item(1, "Milk", 3),
        item(2, "Aspirin", 200, ItemCategory.MEDICATION),
        item(3, "Bread", -1),
        item(4, "Old jam", 20, usedAt = Instant.parse("2026-03-10T00:00:00Z")),
    )

    private fun viewModel(
        repo: FakeExpiryItemRepository = FakeExpiryItemRepository(sample),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = AllItemsViewModel(handle, repo, clock)

    private suspend fun AllItemsViewModel.content(
        predicate: (AllItemsUiState.Content) -> Boolean = { true },
    ) = uiState.first { it is AllItemsUiState.Content && predicate(it) } as AllItemsUiState.Content

    private fun AllItemsUiState.Content.names() = items.map { it.item.name }

    @Test
    fun default_showsActiveItems_earliestExpiryFirst_withoutUsedOnes() = runTest {
        val content = viewModel().content()

        assertEquals(listOf("Bread", "Milk", "Aspirin"), content.names())
        assertEquals(ItemSort.EXPIRY_DATE, content.sort)
        assertFalse(content.showUsed)
        assertEquals(3, content.modeTotal)
        assertTrue(content.hasUsedItems)
    }

    @Test
    fun sorting_changesOrder_andIsRemembered() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle = handle)

        vm.onSortSelected(ItemSort.NAME)

        assertEquals(listOf("Aspirin", "Bread", "Milk"), vm.content { it.sort == ItemSort.NAME }.names())
        // A new ViewModel on the same handle (rotation, process death) keeps the choice.
        assertEquals(ItemSort.NAME, viewModel(handle = handle).content().sort)
    }

    @Test
    fun searchAndCategory_combine() = runTest {
        val vm = viewModel()

        vm.onQueryChange("a")
        vm.onCategorySelected(ItemCategory.FOOD)

        // "a" matches Bread and Aspirin and Milk? Only names containing "a": Bread, Aspirin. FOOD keeps Bread.
        assertEquals(listOf("Bread"), vm.content { it.filter.category == ItemCategory.FOOD && it.filter.query == "a" }.names())
    }

    @Test
    fun noMatches_keepsModeTotal_soTheScreenCanSayNoResults() = runTest {
        val vm = viewModel()

        vm.onQueryChange("zzz")

        val content = vm.content { it.filter.query == "zzz" }
        assertTrue(content.items.isEmpty())
        assertEquals(3, content.modeTotal)
    }

    @Test
    fun clearFilters_resetsSearchAndCategory() = runTest {
        val vm = viewModel()
        vm.onQueryChange("milk")
        vm.onCategorySelected(ItemCategory.FOOD)

        vm.onClearFilters()

        val content = vm.content { !it.filter.isActive }
        assertEquals(3, content.items.size)
    }

    @Test
    fun usedMode_showsOnlyUsedItems() = runTest {
        val vm = viewModel()

        vm.onShowUsedChange(true)

        val content = vm.content { it.showUsed }
        assertEquals(listOf("Old jam"), content.names())
        assertEquals(1, content.modeTotal)
    }

    @Test
    fun emptyRepository_reportsZeroTotal() = runTest {
        val content = viewModel(FakeExpiryItemRepository()).content()

        assertTrue(content.items.isEmpty())
        assertEquals(0, content.modeTotal)
        assertFalse(content.hasUsedItems)
    }

    @Test
    fun loadFailure_isError_andRetryRecovers() = runTest {
        val repo = FakeExpiryItemRepository(sample)
        repo.failObserve = true
        val vm = viewModel(repo)
        assertEquals(AllItemsUiState.Error, vm.uiState.first { it !is AllItemsUiState.Loading })

        repo.failObserve = false
        vm.retry()

        assertEquals(3, vm.content().items.size)
    }

    @Test
    fun queryIsCappedAt100Characters() = runTest {
        val vm = viewModel()
        vm.onQueryChange("x".repeat(150))
        assertEquals(100, vm.query.value.length)
    }
}
