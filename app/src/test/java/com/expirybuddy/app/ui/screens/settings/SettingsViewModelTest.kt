package com.expirybuddy.app.ui.screens.settings

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.FakeUserPreferences
import com.expirybuddy.app.testing.InMemoryReminderScheduler
import com.expirybuddy.app.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-03-08T00:00:00Z"), ZoneOffset.UTC)
    private val preferences = FakeUserPreferences(defaultReminderDays = 3)
    private val scheduler = InMemoryReminderScheduler(clock)

    private fun item(id: Long, daysBefore: Int?, minute: Int? = null) = ExpiryItem(
        id = id,
        name = "Item $id",
        category = ItemCategory.FOOD,
        expiryDate = LocalDate.of(2026, 3, 10),
        reminderDaysBefore = daysBefore,
        reminderMinuteOfDay = minute,
        createdAt = Instant.EPOCH,
    )

    private fun viewModel(items: List<ExpiryItem> = emptyList()) =
        SettingsViewModel(preferences, FakeExpiryItemRepository(items), scheduler)

    @Test
    fun startsWithTheSavedDefaults() {
        val vm = viewModel()
        assertEquals(3, vm.defaultReminderDays.value)
        assertEquals(540, vm.defaultReminderMinutes.value)
    }

    @Test
    fun changingTheDefault_isSavedAndExposed() {
        val vm = viewModel()

        vm.onDefaultReminderChange(7)

        assertEquals(7, vm.defaultReminderDays.value)
        assertEquals(7, preferences.defaultReminderDays)
    }

    @Test
    fun noReminder_isAValidDefault() {
        val vm = viewModel()

        vm.onDefaultReminderChange(null)

        assertNull(vm.defaultReminderDays.value)
        assertNull(preferences.defaultReminderDays)
    }

    @Test
    fun changingTheDefaultTime_isSavedExposedAndReplansReminders() {
        val follower = item(1, 1)
        val own = item(2, 1, minute = 6 * 60)
        val vm = viewModel(listOf(follower, own))
        scheduler.defaultTime = LocalTime.of(9, 0)
        scheduler.schedule(follower)
        scheduler.schedule(own)
        assertEquals(Duration.ofHours(33), scheduler.pending["reminder-item-1"])

        scheduler.defaultTime = LocalTime.of(20, 0)
        vm.onDefaultTimeChange(20 * 60)

        assertEquals(20 * 60, vm.defaultReminderMinutes.value)
        assertEquals(20 * 60, preferences.defaultReminderMinutes)
        assertTrue("rescheduleAll was called", "rescheduleAll" in scheduler.calls)
        assertEquals("follows the new default", Duration.ofHours(44), scheduler.pending["reminder-item-1"])
        assertEquals("keeps its own time", Duration.ofHours(30), scheduler.pending["reminder-item-2"])
        assertEquals("no duplicates", 2, scheduler.pending.size)
    }

    @Test
    fun choosingTheSameTimeAgain_doesNothing() {
        val vm = viewModel(listOf(item(1, 1)))

        vm.onDefaultTimeChange(9 * 60)

        assertFalse("rescheduleAll" in scheduler.calls)
    }

    @Test
    fun changingTheAdvanceDefault_doesNotTouchExistingReminders() {
        val vm = viewModel(listOf(item(1, 1)))

        vm.onDefaultReminderChange(2)

        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun theBuiltInDefault_isOneDayBefore() {
        assertEquals(1, com.expirybuddy.app.domain.model.ReminderOptions.DEFAULT_DAYS_BEFORE)
        val fresh = FakeUserPreferences()
        assertEquals(1, SettingsViewModel(fresh, FakeExpiryItemRepository(), scheduler).defaultReminderDays.value)
    }
}
