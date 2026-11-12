package com.expirybuddy.app.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.validation.DateError
import com.expirybuddy.app.domain.validation.ItemFormValidator
import com.expirybuddy.app.domain.validation.NameError
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.FakePhotoStorage
import com.expirybuddy.app.testing.FakeUserPreferences
import com.expirybuddy.app.testing.MainDispatcherRule
import com.expirybuddy.app.ui.navigation.Destinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddEditViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-03-01T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = FakeExpiryItemRepository()
    private val photos = FakePhotoStorage()
    private val preferences = FakeUserPreferences()

    private fun newViewModel(itemId: Long = Destinations.NO_ITEM_ID) = AddEditViewModel(
        SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to itemId)),
        repository,
        clock,
        photos,
        preferences,
    )

    private fun existingItem() = ExpiryItem(
        id = 7,
        name = "Aspirin",
        category = ItemCategory.MEDICATION,
        expiryDate = LocalDate.of(2026, 9, 1),
        notes = "Cabinet",
        reminderDaysBefore = 7,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @Test
    fun newItem_startsEmptyWithDefaultReminder() {
        val state = newViewModel().uiState.value
        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertNull(state.expiryDate)
        assertEquals(3, state.reminderDaysBefore)
    }

    @Test
    fun save_withBlankName_showsErrorAndSavesNothing() {
        val vm = newViewModel()
        vm.onNameChange("   ")
        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))

        vm.onSave()

        assertEquals(NameError.BLANK, vm.uiState.value.nameError)
        assertFalse(vm.uiState.value.saved)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun save_withoutDate_showsDateError() {
        val vm = newViewModel()
        vm.onNameChange("Milk")

        vm.onSave()

        assertEquals(DateError.MISSING, vm.uiState.value.dateError)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun editingAField_clearsItsError() {
        val vm = newViewModel()
        vm.onSave()
        assertEquals(NameError.BLANK, vm.uiState.value.nameError)

        vm.onNameChange("M")
        assertNull(vm.uiState.value.nameError)
        assertEquals(DateError.MISSING, vm.uiState.value.dateError)

        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))
        assertNull(vm.uiState.value.dateError)
    }

    @Test
    fun save_validNewItem_trimsFieldsStampsCreatedAtAndFinishes() {
        val vm = newViewModel()
        vm.onNameChange("  Milk  ")
        vm.onCategoryChange(ItemCategory.FOOD)
        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))
        vm.onNotesChange("   ")
        vm.onReminderChange(null)

        vm.onSave()

        val item = repository.saved.single()
        assertEquals("Milk", item.name)
        assertNull(item.notes)
        assertNull(item.reminderDaysBefore)
        assertEquals(LocalDate.of(2026, 5, 1), item.expiryDate)
        assertEquals(now, item.createdAt)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun nameAndNotes_areCappedAtTheirLimits() {
        val vm = newViewModel()
        vm.onNameChange("a".repeat(ItemFormValidator.MAX_NAME_LENGTH + 20))
        vm.onNotesChange("b".repeat(ItemFormValidator.MAX_NOTES_LENGTH + 20))

        assertEquals(ItemFormValidator.MAX_NAME_LENGTH, vm.uiState.value.name.length)
        assertEquals(ItemFormValidator.MAX_NOTES_LENGTH, vm.uiState.value.notes.length)
    }

    @Test
    fun editMode_loadsExistingValues() {
        repository.run { kotlinx.coroutines.runBlocking { save(existingItem().copy(id = 0)) } }
        val vm = newViewModel(itemId = 1)

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("Aspirin", state.name)
        assertEquals(ItemCategory.MEDICATION, state.category)
        assertEquals(LocalDate.of(2026, 9, 1), state.expiryDate)
        assertEquals("Cabinet", state.notes)
        assertEquals(7, state.reminderDaysBefore)
    }

    @Test
    fun editMode_save_keepsIdAndCreatedAt() {
        val stored = existingItem().copy(id = 0)
        val id = kotlinx.coroutines.runBlocking { repository.save(stored) }
        val vm = newViewModel(itemId = id)

        vm.onNameChange("Aspirin 500")
        vm.onSave()

        val item = repository.saved.single()
        assertEquals(id, item.id)
        assertEquals("Aspirin 500", item.name)
        assertEquals(stored.createdAt, item.createdAt)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun editMode_missingItem_reportsNotFoundAndBlocksSave() {
        val vm = newViewModel(itemId = 99)

        assertEquals(LoadFailure.NOT_FOUND, vm.uiState.value.loadFailure)
        vm.onSave()
        assertTrue(repository.saved.isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun editMode_loadError_reportsError() {
        repository.failGet = true
        val vm = newViewModel(itemId = 1)

        assertEquals(LoadFailure.ERROR, vm.uiState.value.loadFailure)
        assertFalse(vm.uiState.value.isLoading)
    }

    private fun filledViewModel(reminder: Int?): AddEditViewModel = newViewModel().also {
        it.onNameChange("Milk")
        it.onExpiryDateChange(LocalDate.of(2026, 5, 1))
        it.onReminderChange(reminder)
    }

    @Test
    fun save_withReminderAndExplanationWanted_asksFirstAndDoesNotSaveYet() {
        val vm = filledViewModel(reminder = 3)

        vm.onSave(canExplainNotifications = true)

        assertTrue(vm.uiState.value.showNotificationExplanation)
        assertTrue(repository.saved.isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun handlingTheExplanation_savesTheItem_whateverThePermissionOutcome() {
        val vm = filledViewModel(reminder = 3)
        vm.onSave(canExplainNotifications = true)

        vm.onNotificationExplanationHandled()

        assertFalse(vm.uiState.value.showNotificationExplanation)
        assertTrue(vm.uiState.value.saved)
        assertEquals(3, repository.saved.single().reminderDaysBefore)
    }

    @Test
    fun save_withNoReminder_neverAsksAboutNotifications() {
        val vm = filledViewModel(reminder = null)

        vm.onSave(canExplainNotifications = true)

        assertFalse(vm.uiState.value.showNotificationExplanation)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun save_whenExplanationNotWanted_savesImmediately() {
        val vm = filledViewModel(reminder = 3)

        vm.onSave(canExplainNotifications = false)

        assertFalse(vm.uiState.value.showNotificationExplanation)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun save_withInvalidForm_showsErrorsInsteadOfTheExplanation() {
        val vm = newViewModel()
        vm.onReminderChange(3)

        vm.onSave(canExplainNotifications = true)

        assertFalse(vm.uiState.value.showNotificationExplanation)
        assertEquals(NameError.BLANK, vm.uiState.value.nameError)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun handlingTheExplanation_whenNoneIsShowing_doesNothing() {
        val vm = filledViewModel(reminder = 3)

        vm.onNotificationExplanationHandled()

        assertTrue(repository.saved.isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun savingTwiceWhileExplanationIsShowing_savesOnlyOnce() {
        val vm = filledViewModel(reminder = 3)
        vm.onSave(canExplainNotifications = true)
        vm.onSave(canExplainNotifications = true)

        vm.onNotificationExplanationHandled()

        assertEquals(1, repository.saved.size)
    }

    // --- V2: default reminder, photos, used state, delete ---------------------------------------

    @Test
    fun newItem_startsWithTheDefaultReminderFromSettings() {
        preferences.defaultReminderDays = 7
        assertEquals(7, newViewModel().uiState.value.reminderDaysBefore)

        preferences.defaultReminderDays = null
        assertNull(newViewModel().uiState.value.reminderDaysBefore)
    }

    @Test
    fun editMode_keepsTheItemsOwnReminder_notTheDefault() {
        preferences.defaultReminderDays = 14
        val id = kotlinx.coroutines.runBlocking { repository.save(existingItem().copy(id = 0)) }

        assertEquals(7, newViewModel(itemId = id).uiState.value.reminderDaysBefore)
    }

    @Test
    fun pickingAPhoto_storesItAndShowsIt() {
        val vm = newViewModel()

        vm.onPhotoPicked("content://gallery/1")

        val state = vm.uiState.value
        assertEquals(photos.stored.single(), state.photoUri)
        assertFalse(state.isPhotoBusy)
        assertFalse(state.photoFailed)
    }

    @Test
    fun anUnreadablePhoto_isReported_andKeepsTheCurrentOne() {
        val vm = newViewModel()
        vm.onPhotoPicked("content://gallery/1")
        val first = vm.uiState.value.photoUri
        photos.failImport = true

        vm.onPhotoPicked("content://gallery/broken")

        assertTrue(vm.uiState.value.photoFailed)
        assertEquals(first, vm.uiState.value.photoUri)
        assertTrue(photos.deleted.isEmpty())
    }

    @Test
    fun replacingAnUnsavedPhoto_deletesTheOldFile() {
        val vm = newViewModel()
        vm.onPhotoPicked("content://gallery/1")
        val first = vm.uiState.value.photoUri

        vm.onPhotoPicked("content://gallery/2")

        assertEquals(listOf(first), photos.deleted)
        assertEquals(1, photos.stored.size)
    }

    @Test
    fun removingAnUnsavedPhoto_deletesTheFile() {
        val vm = newViewModel()
        vm.onPhotoPicked("content://gallery/1")
        val first = vm.uiState.value.photoUri

        vm.onPhotoRemoved()

        assertNull(vm.uiState.value.photoUri)
        assertEquals(listOf(first), photos.deleted)
    }

    @Test
    fun savingKeepsThePhoto_onTheItem() {
        val vm = newViewModel()
        vm.onNameChange("Milk")
        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))
        vm.onPhotoPicked("content://gallery/1")

        vm.onSave()

        assertEquals(photos.stored.single(), repository.saved.single().photoUri)
        assertTrue(photos.deleted.isEmpty())
    }

    @Test
    fun leavingWithoutSaving_discardsTheUnsavedPhoto() {
        val vm = newViewModel()
        vm.onPhotoPicked("content://gallery/1")

        vm.discardUnsavedPhoto()

        assertTrue(photos.stored.isEmpty())
    }

    @Test
    fun leavingAfterSaving_keepsThePhoto() {
        val vm = newViewModel()
        vm.onNameChange("Milk")
        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))
        vm.onPhotoPicked("content://gallery/1")
        vm.onSave()

        vm.discardUnsavedPhoto()

        assertEquals(1, photos.stored.size)
        assertTrue(photos.deleted.isEmpty())
    }

    @Test
    fun editMode_removingTheSavedPhoto_deletesItOnlyAfterSaving() {
        val id = kotlinx.coroutines.runBlocking {
            repository.save(existingItem().copy(id = 0, photoUri = "file:///photos/saved.jpg"))
        }
        val vm = newViewModel(itemId = id)
        assertEquals("file:///photos/saved.jpg", vm.uiState.value.photoUri)

        vm.onPhotoRemoved()
        // Not saved yet: the item still points at the file, so the file must still exist.
        assertTrue(photos.deleted.isEmpty())

        vm.onSave()

        assertNull(repository.saved.single().photoUri)
        assertEquals(listOf("file:///photos/saved.jpg"), photos.deleted)
    }

    @Test
    fun editMode_replacingTheSavedPhoto_deletesTheOldOneAfterSaving() {
        val id = kotlinx.coroutines.runBlocking {
            repository.save(existingItem().copy(id = 0, photoUri = "file:///photos/saved.jpg"))
        }
        val vm = newViewModel(itemId = id)

        vm.onPhotoPicked("content://gallery/1")
        vm.onSave()

        assertEquals(photos.stored.single(), repository.saved.single().photoUri)
        assertEquals(listOf("file:///photos/saved.jpg"), photos.deleted)
    }

    @Test
    fun editMode_save_keepsUsedState_andStampsUpdatedAt() {
        val usedAt = Instant.parse("2026-02-01T00:00:00Z")
        val id = kotlinx.coroutines.runBlocking { repository.save(existingItem().copy(id = 0, usedAt = usedAt)) }
        val vm = newViewModel(itemId = id)

        vm.onNameChange("Aspirin 500")
        vm.onSave()

        val item = repository.saved.single()
        assertEquals(usedAt, item.usedAt)
        assertEquals(now, item.updatedAt)
    }

    @Test
    fun editMode_delete_removesItemAndItsPhoto() {
        val id = kotlinx.coroutines.runBlocking {
            repository.save(existingItem().copy(id = 0, photoUri = "file:///photos/saved.jpg"))
        }
        val vm = newViewModel(itemId = id)

        vm.onDelete()

        assertTrue(vm.uiState.value.deleted)
        assertTrue(repository.saved.isEmpty())
        assertEquals(listOf("file:///photos/saved.jpg"), photos.deleted)
    }

    @Test
    fun editMode_deleteFailure_keepsItem_andFlagsIt() {
        val id = kotlinx.coroutines.runBlocking { repository.save(existingItem().copy(id = 0)) }
        val vm = newViewModel(itemId = id)
        repository.failDelete = true

        vm.onDelete()

        assertTrue(vm.uiState.value.deleteFailed)
        assertFalse(vm.uiState.value.deleted)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun addMode_delete_doesNothing() {
        val vm = newViewModel()

        vm.onDelete()

        assertFalse(vm.uiState.value.deleted)
    }

    @Test
    fun save_failure_surfacesErrorAndAllowsRetry() {
        repository.failSave = true
        val vm = newViewModel()
        vm.onNameChange("Milk")
        vm.onExpiryDateChange(LocalDate.of(2026, 5, 1))

        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.saved)

        repository.failSave = false
        vm.onSave()
        assertFalse(vm.uiState.value.saveFailed)
        assertTrue(vm.uiState.value.saved)
    }
}
