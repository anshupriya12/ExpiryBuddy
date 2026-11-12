package com.expirybuddy.app.data.repository

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.reminders.ReminderWork
import com.expirybuddy.app.testing.FakeExpiryItemRepository
import com.expirybuddy.app.testing.InMemoryReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Checks that saving, editing and deleting keep reminders in step, end to end through the decorator. */
class ReminderSyncingRepositoryTest {
    // "Now" is 1 March 2026, 08:00 UTC.
    private val clock = Clock.fixed(Instant.parse("2026-03-01T08:00:00Z"), ZoneOffset.UTC)
    private val delegate = FakeExpiryItemRepository()
    private val scheduler = InMemoryReminderScheduler(clock)
    private val repository = ReminderSyncingRepository(delegate, scheduler)

    private fun item(expiry: LocalDate = LocalDate.of(2026, 3, 10), daysBefore: Int? = 3, id: Long = ExpiryItem.NEW_ID) =
        ExpiryItem(
            id = id,
            name = "Milk",
            category = ItemCategory.FOOD,
            expiryDate = expiry,
            reminderDaysBefore = daysBefore,
            createdAt = Instant.EPOCH,
        )

    @Test
    fun savingANewItem_schedulesItsReminderUnderTheAssignedId() = runTest {
        val id = repository.save(item())

        // Reminder: 7 March 09:00, which is 6 days 1 hour after "now".
        assertEquals(mapOf(ReminderWork.uniqueName(id) to Duration.ofDays(6).plusHours(1)), scheduler.pending)
    }

    @Test
    fun markingAnItemAsUsed_cancelsItsPendingReminder() = runTest {
        val id = repository.save(item(daysBefore = 3))
        assertEquals(1, scheduler.pending.size)

        repository.save(item(daysBefore = 3, id = id).copy(usedAt = Instant.parse("2026-03-01T08:00:00Z")))

        assertTrue(scheduler.pending.isEmpty())
        // The item is still saved: marking as used never deletes it.
        assertEquals(1, delegate.saved.size)
    }

    @Test
    fun restoringAUsedItem_schedulesItsReminderAgain() = runTest {
        val id = repository.save(item(daysBefore = 3).copy(usedAt = Instant.parse("2026-03-01T08:00:00Z")))
        assertTrue(scheduler.pending.isEmpty())

        repository.save(item(daysBefore = 3, id = id))

        assertEquals(Duration.ofDays(6).plusHours(1), scheduler.pending[ReminderWork.uniqueName(id)])
    }

    @Test
    fun restoreAtAppStart_skipsUsedItems() = runTest {
        val active = item(daysBefore = 3, id = 1)
        val used = item(daysBefore = 3, id = 2).copy(usedAt = Instant.parse("2026-03-01T08:00:00Z"))

        scheduler.restore(listOf(active, used))

        assertEquals(setOf(ReminderWork.uniqueName(1)), scheduler.pending.keys)
    }

    @Test
    fun savingWithoutAReminder_schedulesNothing() = runTest {
        repository.save(item(daysBefore = null))
        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun editingAnItem_replacesItsPreviousReminder() = runTest {
        val id = repository.save(item(daysBefore = 3))

        repository.save(item(daysBefore = 7, id = id)) // reminder moves to 3 March 09:00

        assertEquals(1, scheduler.pending.size)
        assertEquals(Duration.ofDays(2).plusHours(1), scheduler.pending[ReminderWork.uniqueName(id)])
    }

    @Test
    fun editingTheExpiryDate_movesTheReminder() = runTest {
        val id = repository.save(item(expiry = LocalDate.of(2026, 3, 10)))

        repository.save(item(expiry = LocalDate.of(2026, 3, 20), id = id)) // reminder now 17 March

        assertEquals(Duration.ofDays(16).plusHours(1), scheduler.pending[ReminderWork.uniqueName(id)])
        assertEquals(1, scheduler.pending.size)
    }

    @Test
    fun editingToNoReminder_cancelsTheOldOne() = runTest {
        val id = repository.save(item(daysBefore = 3))
        assertEquals(1, scheduler.pending.size)

        repository.save(item(daysBefore = null, id = id))

        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun editingSoTheReminderDateIsInThePast_cancelsTheOldOne() = runTest {
        val id = repository.save(item(expiry = LocalDate.of(2026, 3, 10), daysBefore = 3))
        assertEquals(1, scheduler.pending.size)

        // Expiry moves to 2 March with a 3-day reminder: the reminder date, 27 February, has passed.
        repository.save(item(expiry = LocalDate.of(2026, 3, 2), daysBefore = 3, id = id))

        assertNull(scheduler.pending[ReminderWork.uniqueName(id)])
    }

    @Test
    fun deletingAnItem_cancelsItsReminder() = runTest {
        val id = repository.save(item())
        assertEquals(1, scheduler.pending.size)

        assertTrue(repository.delete(id))

        assertTrue(scheduler.pending.isEmpty())
        assertEquals("cancel:$id", scheduler.calls.last())
    }

    @Test
    fun deletingAMissingItem_stillCancelsAndReportsFalse() = runTest {
        assertFalse(repository.delete(99))
        assertEquals(listOf("cancel:99"), scheduler.calls)
    }

    @Test
    fun remindersForDifferentItems_areIndependent() = runTest {
        val first = repository.save(item(daysBefore = 3))
        val second = repository.save(item(daysBefore = 1))

        repository.delete(first)

        assertNull(scheduler.pending[ReminderWork.uniqueName(first)])
        assertNotNull(scheduler.pending[ReminderWork.uniqueName(second)])
    }

    @Test
    fun aSchedulerFailure_doesNotFailTheSave() = runTest {
        scheduler.failWith = IllegalStateException("WorkManager unavailable")

        val id = repository.save(item())

        assertEquals(item(id = id), delegate.getItem(id))
    }

    @Test
    fun aSchedulerFailure_doesNotFailTheDelete() = runTest {
        val id = repository.save(item())
        scheduler.failWith = IllegalStateException("WorkManager unavailable")

        assertTrue(repository.delete(id))
        assertNull(delegate.getItem(id))
    }

    @Test
    fun aFailedSave_doesNotTouchReminders() = runTest {
        delegate.failSave = true

        val failure = runCatching { repository.save(item()) }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun reads_passThroughToTheDelegate() = runTest {
        val id = repository.save(item())
        assertEquals(id, repository.getItem(id)?.id)
        assertEquals(listOf(id), repository.getAllItems().map { it.id })
    }

    @Test
    fun uniqueWorkNames_areStablePerItemAndDistinctAcrossItems() {
        assertEquals(ReminderWork.uniqueName(5), ReminderWork.uniqueName(5))
        assertTrue(ReminderWork.uniqueName(5) != ReminderWork.uniqueName(6))
    }

    // --- V2.2: preferences and edits ------------------------------------------------------------

    @Test
    fun editingTheUseByDate_replacesTheReminder_withoutAddingAnotherJob() = runTest {
        val id = repository.save(item(daysBefore = 3))

        repository.save(item(expiry = LocalDate.of(2026, 3, 20), daysBefore = 3, id = id))

        // 17 March 09:00 is 16 days 1 hour after "now": one job, new time.
        assertEquals(mapOf(ReminderWork.uniqueName(id) to Duration.ofDays(16).plusHours(1)), scheduler.pending)
    }

    @Test
    fun changingTheItemNoticeAndTime_replacesTheReminder() = runTest {
        val id = repository.save(item(daysBefore = 3))

        repository.save(item(daysBefore = 1, id = id).copy(reminderMinuteOfDay = 19 * 60 + 30))

        // 9 March 19:30 is 8 days 11 hours 30 minutes after 1 March 08:00.
        assertEquals(
            mapOf(ReminderWork.uniqueName(id) to Duration.ofDays(8).plusHours(11).plusMinutes(30)),
            scheduler.pending,
        )
    }

    @Test
    fun usesTheDefaultTimeForItemsWithoutTheirOwn() = runTest {
        scheduler.defaultTime = java.time.LocalTime.of(7, 0)

        val id = repository.save(item(daysBefore = 3))

        // 7 March 07:00 = 5 days 23 hours after now.
        assertEquals(mapOf(ReminderWork.uniqueName(id) to Duration.ofDays(5).plusHours(23)), scheduler.pending)
    }

    @Test
    fun aReminderMomentThatAlreadyPassed_isNotScheduled() = runTest {
        // Due 2 March, "1 day before" is 1 March; at 07:00 that is before "now" (08:00).
        val id = repository.save(item(expiry = LocalDate.of(2026, 3, 2), daysBefore = 1).copy(reminderMinuteOfDay = 7 * 60))

        assertTrue(scheduler.pending.isEmpty())
        assertNotNull(delegate.getItem(id))
    }

    @Test
    fun savingTheSameItemRepeatedly_neverCreatesDuplicateJobs() = runTest {
        val id = repository.save(item())
        repeat(5) { repository.save(item(id = id)) }

        assertEquals(1, scheduler.pending.size)
    }

    @Test
    fun deletingAfterV22Edits_cancelsItsReminder() = runTest {
        val id = repository.save(item())
        repository.delete(id)

        assertTrue(scheduler.pending.isEmpty())
    }
}
