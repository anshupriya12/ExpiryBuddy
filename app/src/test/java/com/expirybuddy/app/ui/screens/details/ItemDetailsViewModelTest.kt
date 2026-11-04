package com.expirybuddy.app.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.MainDispatcherRule
import com.expirybuddy.app.ui.navigation.Destinations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ItemDetailsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val item = ExpiryItem(
        id = 4,
        name = "Passport",
        category = ItemCategory.DOCUMENT,
        expiryDate = LocalDate.of(2030, 1, 1),
        createdAt = Instant.EPOCH,
    )
    private val repository = FakeExpiryItemRepository(listOf(item))

    private fun viewModel(id: Long) =
        ItemDetailsViewModel(SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to id)), repository)

    @Test
    fun existingItem_isFound() = runTest {
        val state = viewModel(4).uiState.first { it !is DetailsUiState.Loading }
        assertEquals(DetailsUiState.Found(item), state)
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
    fun delete_removesItemAndEndsInDeleted_neverNotFound() = runTest {
        val vm = viewModel(4)
        val seen = mutableListOf<DetailsUiState>()
        vm.uiState.first { it is DetailsUiState.Found }

        vm.onDelete()
        seen += vm.uiState.first { it is DetailsUiState.Deleted || it is DetailsUiState.NotFound }

        assertEquals(DetailsUiState.Deleted, seen.single())
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun deleteFailure_keepsItemAndFlagsError() = runTest {
        repository.failDelete = true
        val vm = viewModel(4)
        vm.uiState.first { it is DetailsUiState.Found }

        vm.onDelete()

        val state = vm.uiState.first { it is DetailsUiState.Found && it.deleteFailed }
        assertEquals(DetailsUiState.Found(item, deleteFailed = true), state)
        assertEquals(1, repository.saved.size)
    }
}
