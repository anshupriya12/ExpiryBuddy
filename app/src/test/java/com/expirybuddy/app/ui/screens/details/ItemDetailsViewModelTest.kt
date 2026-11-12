package com.expirybuddy.app.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.FakePhotoStorage
import com.expirybuddy.app.testing.MainDispatcherRule
import com.expirybuddy.app.testing.MutableClock
import com.expirybuddy.app.ui.navigation.Destinations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ItemDetailsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-03-01T10:00:00Z")
    private val clock = MutableClock(now)
    private val photos = FakePhotoStorage()

    private val item = ExpiryItem(
        id = 4,
        name = "Passport",
        category = ItemCategory.OTHER,
        expiryDate = LocalDate.of(2030, 1, 1),
        reminderDaysBefore = 7,
        createdAt = Instant.EPOCH,
        photoUri = "file:///photos/passport.jpg",
    )
    private val repository = FakeExpiryItemRepository(listOf(item))

    private fun viewModel(id: Long) =
        ItemDetailsViewModel(SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to id)), repository, clock, photos)

    private suspend fun ItemDetailsViewModel.found(): DetailsUiState.Found =
        uiState.first { it is DetailsUiState.Found } as DetailsUiState.Found

    @Test
    fun existingItem_isFound_withStatusFromTodaysDate() = runTest {
        val found = viewModel(4).found()

        assertEquals(item, found.item)
        assertEquals(ExpiryStatus.UPCOMING, found.status)
        // 1 March 2026 -> 1 January 2030.
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2026, 3, 1), item.expiryDate), found.daysUntilExpiry)
    }

    @Test
    fun missingItem_isNotFound() = runTest {
        val state = viewModel(404).uiState.first { it !is DetailsUiState.Loading }
        assertEquals(DetailsUiState.NotFound, state)
    }

    @Test
    fun observeFailure_isError() = runTest {
        repository.failObserve = true
        val state = viewModel(4).uiState.first { it !is DetailsUiState.Loading }
        assertEquals(DetailsUiState.Error, state)
    }

    @Test
    fun delete_removesItemAndItsPhoto_andEndsInDeleted_neverNotFound() = runTest {
        val vm = viewModel(4)
        val seen = mutableListOf<DetailsUiState>()
        vm.found()

        vm.onDelete()
        seen += vm.uiState.first { it is DetailsUiState.Deleted || it is DetailsUiState.NotFound }

        assertEquals(DetailsUiState.Deleted, seen.single())
        assertTrue(repository.saved.isEmpty())
        assertEquals(listOf("file:///photos/passport.jpg"), photos.deleted)
    }

    @Test
    fun deleteFailure_keepsItemAndPhoto_andFlagsError() = runTest {
        repository.failDelete = true
        val vm = viewModel(4)
        vm.found()

        vm.onDelete()

        val state = vm.uiState.first { it is DetailsUiState.Found && it.deleteFailed } as DetailsUiState.Found
        assertEquals(item, state.item)
        assertEquals(1, repository.saved.size)
        assertTrue(photos.deleted.isEmpty())
    }

    @Test
    fun markUsed_keepsTheItem_stampsUsedAt_andEndsInMarkedUsed() = runTest {
        val vm = viewModel(4)
        vm.found()

        vm.onMarkUsed()

        assertEquals(DetailsUiState.MarkedUsed, vm.uiState.first { it is DetailsUiState.MarkedUsed })
        val stored = repository.saved.single()
        assertEquals(now, stored.usedAt)
        assertEquals(now, stored.updatedAt)
        assertTrue(stored.isUsed)
        // Nothing is deleted: the item and its photo are both still there.
        assertTrue(photos.deleted.isEmpty())
        assertEquals(item.photoUri, stored.photoUri)
    }

    @Test
    fun markUsed_failure_leavesItemActive_andFlagsError() = runTest {
        repository.failSave = true
        val vm = viewModel(4)
        vm.found()

        vm.onMarkUsed()

        val state = vm.uiState.first { it is DetailsUiState.Found && it.updateFailed } as DetailsUiState.Found
        assertFalse(state.item.isUsed)
        assertNull(repository.saved.single().usedAt)
    }

    @Test
    fun restore_makesAUsedItemActiveAgain() = runTest {
        repository.save(item.copy(usedAt = Instant.parse("2026-02-01T00:00:00Z")))
        val vm = viewModel(4)
        assertTrue(vm.found().item.isUsed)

        vm.onRestore()

        val restored = vm.uiState.first { it is DetailsUiState.Found && !it.item.isUsed } as DetailsUiState.Found
        assertNull(restored.item.usedAt)
        assertEquals(now, repository.saved.single().updatedAt)
    }

    @Test
    fun markUsed_onAnAlreadyUsedItem_doesNothing() = runTest {
        val used = item.copy(usedAt = Instant.parse("2026-02-01T00:00:00Z"))
        repository.save(used)
        val vm = viewModel(4)
        vm.found()

        vm.onMarkUsed()

        assertNotNull(repository.saved.single().usedAt)
        assertEquals(used.usedAt, repository.saved.single().usedAt)
    }
}
