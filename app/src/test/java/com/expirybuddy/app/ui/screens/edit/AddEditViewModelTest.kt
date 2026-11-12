package com.expirybuddy.app.ui.screens.edit

import com.expirybuddy.app.domain.model.StorageType
import com.expirybuddy.app.domain.validation.UseWithinError
import com.expirybuddy.app.domain.validation.CustomReminderError
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import androidx.lifecycle.SavedStateHandle
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.model.ReminderOptions
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
        assertNull(state.useByDate)
        assertEquals(ReminderOptions.DEFAULT_DAYS_BEFORE, state.reminderDaysBefore)
    }

    @Test
    fun save_withBlankName_showsErrorAndSavesNothing() {
        val vm = newViewModel()
        vm.onNameChange("   ")
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))

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

        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
        assertNull(vm.uiState.value.dateError)
    }

    @Test
    fun save_validNewItem_trimsFieldsStampsCreatedAtAndFinishes() {
        val vm = newViewModel()
        vm.onNameChange("  Milk  ")
        vm.onCategoryChange(ItemCategory.FOOD)
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
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
        assertEquals(LocalDate.of(2026, 9, 1), state.useByDate)
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
        it.onUseByDateChange(LocalDate.of(2026, 5, 1))
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
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
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
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
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
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))

        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.saved)

        repository.failSave = false
        vm.onSave()
        assertFalse(vm.uiState.value.saveFailed)
        assertTrue(vm.uiState.value.saved)
    }

    // --- V2.2: use-by calculation, reminder preferences ---------------------------------------

    @Test
    fun openedOnPlusUseWithin_fillsTheUseByDateImmediately() {
        val vm = newViewModel()

        vm.onOpenedOnChange(LocalDate.of(2026, 11, 12))
        assertNull("nothing to calculate yet", vm.uiState.value.useByDate)
        vm.onUseWithinChange("2")

        val state = vm.uiState.value
        assertEquals(LocalDate.of(2026, 11, 14), state.useByDate)
        assertTrue(state.useByCalculated)
    }

    @Test
    fun changingTheUnit_orTheOpeningDate_recalculates() {
        val vm = newViewModel()
        vm.onOpenedOnChange(LocalDate.of(2026, 11, 12))
        vm.onUseWithinChange("2")

        vm.onUseWithinUnitChange(UseWithinUnit.MONTHS)
        assertEquals(LocalDate.of(2027, 1, 12), vm.uiState.value.useByDate)

        vm.onOpenedOnChange(LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2027, 1, 20), vm.uiState.value.useByDate)
    }

    @Test
    fun manuallyEditingTheCalculatedDate_isKept_untilTheOpeningDetailsChangeAgain() {
        val vm = newViewModel()
        vm.onOpenedOnChange(LocalDate.of(2026, 11, 12))
        vm.onUseWithinChange("2")

        vm.onUseByDateChange(LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2026, 11, 20), vm.uiState.value.useByDate)
        assertFalse(vm.uiState.value.useByCalculated)

        vm.onUseWithinChange("3")
        assertEquals(LocalDate.of(2026, 11, 15), vm.uiState.value.useByDate)
    }

    @Test
    fun aDirectUseByDate_needsNoOpeningInformation() {
        val vm = newViewModel()
        vm.onNameChange("Rice")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))

        vm.onSave()

        val saved = repository.saved.single()
        assertEquals(LocalDate.of(2026, 12, 1), saved.expiryDate)
        assertNull(saved.openedOn)
        assertNull(saved.useWithinAmount)
        assertNull(saved.storageInstructions)
        assertNull(saved.reminderMinuteOfDay)
    }

    @Test
    fun invalidUseWithin_isReportedAndBlocksSave_withoutOverwritingTheDate() {
        val vm = newViewModel()
        vm.onNameChange("Juice")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))
        vm.onOpenedOnChange(LocalDate.of(2026, 11, 12))
        vm.onUseWithinChange("0")

        assertEquals(LocalDate.of(2026, 12, 1), vm.uiState.value.useByDate)
        vm.onSave()

        assertEquals(UseWithinError.INVALID, vm.uiState.value.useWithinError)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun openingDetailsAndStorage_areSavedAndLoadedBack() {
        val vm = newViewModel()
        vm.onNameChange("Juice")
        vm.onOpenedOnChange(LocalDate.of(2026, 11, 12))
        vm.onUseWithinChange("3")
        vm.onUseWithinUnitChange(UseWithinUnit.DAYS)
        vm.onStorageChange("  Keep refrigerated ")
        vm.onSave()

        val saved = repository.saved.single()
        assertEquals(LocalDate.of(2026, 11, 15), saved.expiryDate)
        assertEquals(LocalDate.of(2026, 11, 12), saved.openedOn)
        assertEquals(3, saved.useWithinAmount)
        assertEquals(UseWithinUnit.DAYS, saved.useWithinUnit)
        assertEquals("Keep refrigerated", saved.storageInstructions)

        val edit = newViewModel(itemId = saved.id).uiState.value
        assertEquals(LocalDate.of(2026, 11, 12), edit.openedOn)
        assertEquals("3", edit.useWithinText)
        assertEquals("Keep refrigerated", edit.storageInstructions)
    }

    @Test
    fun newItem_inheritsTheDefaultNoticeAndTime_andSavesNoItemTimeUnlessChosen() {
        preferences.defaultReminderDays = 2
        preferences.defaultReminderMinutes = 8 * 60
        val vm = newViewModel()
        vm.onNameChange("Milk")
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))

        assertEquals(2, vm.uiState.value.reminderDaysBefore)
        assertEquals(8 * 60, vm.uiState.value.defaultReminderMinutes)
        assertNull(vm.uiState.value.reminderMinuteOfDay)
        vm.onSave()

        assertEquals(2, repository.saved.single().reminderDaysBefore)
        assertNull("follows the default time", repository.saved.single().reminderMinuteOfDay)
    }

    @Test
    fun perItemOverride_ofNoticeAndTime_isSavedAndPreservedWhenEditing() {
        val vm = newViewModel()
        vm.onNameChange("Paneer")
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
        vm.onReminderChange(0)
        vm.onReminderTimeChange(19 * 60)
        vm.onSave()

        val saved = repository.saved.single()
        assertEquals(0, saved.reminderDaysBefore)
        assertEquals(19 * 60, saved.reminderMinuteOfDay)

        // Changing the global defaults later does not disturb the item's own choices.
        preferences.defaultReminderDays = 3
        preferences.defaultReminderMinutes = 7 * 60
        val edit = newViewModel(itemId = saved.id)
        assertEquals(0, edit.uiState.value.reminderDaysBefore)
        assertEquals(19 * 60, edit.uiState.value.reminderMinuteOfDay)
        edit.onNameChange("Paneer (fresh)")
        edit.onSave()
        assertEquals(0, repository.saved.single().reminderDaysBefore)
        assertEquals(19 * 60, repository.saved.single().reminderMinuteOfDay)
    }

    @Test
    fun resettingTheItemTime_goesBackToTheDefault() {
        val vm = newViewModel()
        vm.onNameChange("Milk")
        vm.onUseByDateChange(LocalDate.of(2026, 5, 1))
        vm.onReminderTimeChange(19 * 60)
        vm.onReminderTimeChange(null)
        vm.onSave()

        assertNull(repository.saved.single().reminderMinuteOfDay)
    }

    @Test
    fun customAdvanceNotice_isSavedAsTheTypedNumberOfDays_andReloadedAsCustom() {
        val vm = newViewModel()
        vm.onNameChange("Vitamins")
        vm.onUseByDateChange(LocalDate.of(2026, 9, 1))
        vm.onCustomReminderSelected()
        vm.onCustomReminderTextChange("10")
        vm.onSave()

        assertEquals(10, repository.saved.single().reminderDaysBefore)
        val edit = newViewModel(itemId = repository.saved.single().id).uiState.value
        assertTrue(edit.reminderIsCustom)
        assertEquals("10", edit.customReminderText)
    }

    @Test
    fun customAdvanceNotice_invalidOrEmpty_blocksSave() {
        val vm = newViewModel()
        vm.onNameChange("Vitamins")
        vm.onUseByDateChange(LocalDate.of(2026, 9, 1))
        vm.onCustomReminderSelected()
        vm.onSave()

        assertEquals(CustomReminderError.INVALID, vm.uiState.value.customReminderError)
        assertTrue(repository.saved.isEmpty())

        vm.onCustomReminderTextChange("400")
        vm.onSave()
        assertEquals(CustomReminderError.INVALID, vm.uiState.value.customReminderError)
    }

    @Test
    fun choosingAPresetAfterCustom_leavesCustomMode() {
        val vm = newViewModel()
        vm.onCustomReminderSelected()
        vm.onReminderChange(1)

        assertFalse(vm.uiState.value.reminderIsCustom)
        assertEquals(1, vm.uiState.value.reminderDaysBefore)
    }

    @Test
    fun anOldItemWithAnUncommonNotice_opensAsCustom() {
        repository.saved.size // touch
        val old = existingItem()
        val repo = FakeExpiryItemRepository(listOf(old))
        val vm = AddEditViewModel(
            SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to old.id)), repo, clock, photos, preferences,
        )

        // The V2 item had "7 days before", which is no longer a preset.
        assertTrue(vm.uiState.value.reminderIsCustom)
        assertEquals("7", vm.uiState.value.customReminderText)
    }

    // --- Final refinements: storage and the 1-day default -----------------------------------------

    @Test
    fun storageIsOptional_aNewItemSavesWithNoTypeAndNoText() {
        val vm = newViewModel()
        vm.onNameChange("Rice")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))

        assertNull(vm.uiState.value.storageType)
        vm.onSave()

        assertNull(repository.saved.single().storageType)
        assertNull(repository.saved.single().storageInstructions)
    }

    @Test
    fun storageType_andMultilineInstructions_areSavedAndLoadedBack() {
        val vm = newViewModel()
        vm.onNameChange("Paneer")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))
        vm.onStorageTypeChange(StorageType.FROZEN)
        vm.onStorageChange("Keep tightly sealed.\nStore away from direct sunlight.")
        vm.onSave()

        val saved = repository.saved.single()
        assertEquals(StorageType.FROZEN, saved.storageType)
        assertEquals("Keep tightly sealed.\nStore away from direct sunlight.", saved.storageInstructions)

        val edit = newViewModel(itemId = saved.id).uiState.value
        assertEquals(StorageType.FROZEN, edit.storageType)
        assertEquals("Keep tightly sealed.\nStore away from direct sunlight.", edit.storageInstructions)
    }

    @Test
    fun eachOfTheThreeStorageTypes_canBeChosen_andCleared() {
        val vm = newViewModel()
        StorageType.entries.forEach {
            vm.onStorageTypeChange(it)
            assertEquals(it, vm.uiState.value.storageType)
        }
        vm.onStorageTypeChange(null)
        assertNull(vm.uiState.value.storageType)
    }

    @Test
    fun typeOnly_andTextOnly_areBothValid() {
        val typeOnly = newViewModel()
        typeOnly.onNameChange("Milk")
        typeOnly.onUseByDateChange(LocalDate.of(2026, 12, 1))
        typeOnly.onStorageTypeChange(StorageType.REFRIGERATE)
        typeOnly.onSave()
        assertEquals(StorageType.REFRIGERATE, repository.saved.single().storageType)
        assertNull(repository.saved.single().storageInstructions)

        val textOnly = newViewModel()
        textOnly.onNameChange("Cream")
        textOnly.onUseByDateChange(LocalDate.of(2026, 12, 1))
        textOnly.onStorageChange("Follow the package instructions.")
        textOnly.onSave()
        val cream = repository.saved.first { it.name == "Cream" }
        assertNull(cream.storageType)
        assertEquals("Follow the package instructions.", cream.storageInstructions)
    }

    @Test
    fun editingAnItemWithOldStorageText_keepsItUntouched_andLeavesTheTypeEmpty() {
        val old = existingItem().copy(storageInstructions = "Keep refrigerated")
        val repo = FakeExpiryItemRepository(listOf(old))
        val vm = AddEditViewModel(
            SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to old.id)), repo, clock, photos, preferences,
        )
        assertEquals("Keep refrigerated", vm.uiState.value.storageInstructions)
        assertNull(vm.uiState.value.storageType)

        vm.onNameChange("Aspirin 2")
        vm.onSave()

        assertEquals("Keep refrigerated", repo.saved.single().storageInstructions)
        assertNull(repo.saved.single().storageType)
    }

    @Test
    fun changingTheStorageType_whenEditing_isPreserved() {
        val vm = newViewModel()
        vm.onNameChange("Juice")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))
        vm.onStorageTypeChange(StorageType.AIRTIGHT)
        vm.onSave()
        val id = repository.saved.single().id

        val edit = newViewModel(itemId = id)
        edit.onStorageTypeChange(StorageType.REFRIGERATE)
        edit.onSave()

        assertEquals(StorageType.REFRIGERATE, repository.saved.single().storageType)
    }

    @Test
    fun aNewItem_defaultsToOneDayBefore_whenNothingWasChosenInSettings() {
        val vm = AddEditViewModel(
            SavedStateHandle(mapOf(Destinations.ITEM_ID_ARG to Destinations.NO_ITEM_ID)),
            repository, clock, photos, FakeUserPreferences(),
        )
        assertEquals(1, vm.uiState.value.reminderDaysBefore)
        assertFalse(vm.uiState.value.reminderIsCustom)

        vm.onNameChange("Milk")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))
        vm.onSave()
        assertEquals(1, repository.saved.single().reminderDaysBefore)
    }

    @Test
    fun anExplicitSettingsChoice_winsOverTheNewDefault_andItemOverridesAreKept() {
        preferences.defaultReminderDays = 3
        val vm = newViewModel()
        assertEquals(3, vm.uiState.value.reminderDaysBefore)

        vm.onNameChange("Paneer")
        vm.onUseByDateChange(LocalDate.of(2026, 12, 1))
        vm.onReminderChange(0)
        vm.onSave()
        assertEquals(0, repository.saved.single().reminderDaysBefore)
    }
}

