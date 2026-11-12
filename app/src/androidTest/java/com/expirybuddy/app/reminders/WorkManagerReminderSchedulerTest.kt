package com.expirybuddy.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Exercises the real WorkManager: unique naming, replacement on edit, cancellation on delete. */
@RunWith(AndroidJUnit4::class)
class WorkManagerReminderSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val workManager = WorkManager.getInstance(context)
    private val scheduler = WorkManagerReminderScheduler(workManager, Clock.systemDefaultZone())
    private val itemId = 987_001L
    private val workName = ReminderWork.uniqueName(itemId)
    private val today = LocalDate.now()

    @Before
    fun setUp() = cancelAndWait()

    @After
    fun tearDown() = cancelAndWait()

    private fun cancelAndWait() {
        workManager.cancelUniqueWork(workName).result.get()
    }

    private fun item(expiresInDays: Long, daysBefore: Int?) = ExpiryItem(
        id = itemId,
        name = "Scheduler test",
        category = ItemCategory.OTHER,
        expiryDate = today.plusDays(expiresInDays),
        reminderDaysBefore = daysBefore,
        createdAt = Instant.now(),
    )

    private fun infos(): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(workName).get()

    private fun enqueued(): List<WorkInfo> = infos().filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun schedulingAFutureReminder_enqueuesExactlyOneJobUnderTheStableName() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))

        assertEquals(1, enqueued().size)
    }

    @Test
    fun editingTheReminder_replacesTheOldJob() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))
        val first = enqueued().single()

        scheduler.schedule(item(expiresInDays = 30, daysBefore = 7))

        val current = enqueued().single()
        assertTrue("old job should have been replaced", current.id != first.id)
        // REPLACE cancels and deletes the old job, so it is either gone or cancelled; never still alive.
        assertTrue(
            "old job must not be left pending or running",
            infos().none { it.id == first.id && it.state != WorkInfo.State.CANCELLED },
        )
        // 7 days before is 4 days earlier than 3 days before.
        assertTrue(current.initialDelayMillis < first.initialDelayMillis)
    }

    @Test
    fun cancelling_removesThePendingJob() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))

        scheduler.cancel(itemId)
        workManager.pruneWork().result.get()

        assertTrue(enqueued().isEmpty())
    }

    @Test
    fun markingAsUsed_cancelsThePendingJob_andRestoringSchedulesItAgain() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))
        assertEquals(1, enqueued().size)

        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3).copy(usedAt = Instant.now()))
        assertTrue(enqueued().isEmpty())

        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))
        assertEquals(1, enqueued().size)
    }

    @Test
    fun restore_neverSchedulesUsedItems() {
        scheduler.restore(listOf(item(expiresInDays = 30, daysBefore = 3).copy(usedAt = Instant.now())))
        assertTrue(enqueued().isEmpty())
    }

    @Test
    fun editingToNoReminder_cancelsThePendingJob() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))

        scheduler.schedule(item(expiresInDays = 30, daysBefore = null))

        assertTrue(enqueued().isEmpty())
    }

    @Test
    fun editingSoTheReminderIsInThePast_cancelsThePendingJob() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))

        // Expires tomorrow with a 7-day reminder: that date passed days ago.
        scheduler.schedule(item(expiresInDays = 1, daysBefore = 7))

        assertTrue(enqueued().isEmpty())
    }

    @Test
    fun schedulingAPastReminderForANewItem_enqueuesNothing() {
        scheduler.schedule(item(expiresInDays = -5, daysBefore = 0))

        assertTrue(infos().isEmpty())
    }

    @Test
    fun restore_enqueuesFutureRemindersButNeverCancelsExistingOnes() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))
        val before = enqueued().single()

        // A past reminder for the same item must not cancel what is already pending.
        scheduler.restore(listOf(item(expiresInDays = -5, daysBefore = 0)))
        assertEquals(before.id, enqueued().single().id)

        // A future reminder is (re)enqueued.
        scheduler.restore(listOf(item(expiresInDays = 40, daysBefore = 3)))
        assertEquals(1, enqueued().size)
    }

    @Test
    fun theInitialDelay_matchesTheReminderMoment() {
        scheduler.schedule(item(expiresInDays = 30, daysBefore = 3))

        val delayDays = enqueued().single().initialDelayMillis / (24 * 60 * 60 * 1000.0)
        // Reminder is 27 days out at 09:00; the delay is within a day of 27 days from now.
        assertTrue("delay was $delayDays days", delayDays in 26.0..28.0)
    }

    // --- V2.2: configurable time of day ---------------------------------------------------------

    private fun schedulerWithDefault(time: java.time.LocalTime) =
        WorkManagerReminderScheduler(workManager, Clock.systemDefaultZone()) { time }

    private fun delayMillisFor(item: ExpiryItem, defaultTime: java.time.LocalTime): Long? =
        com.expirybuddy.app.domain.reminder.ReminderCalculator
            .delayUntilReminder(item, java.time.ZonedDateTime.now(), defaultTime)?.toMillis()

    @Test
    fun theJobDelay_followsTheConfiguredDefaultTime() {
        val item = item(expiresInDays = 30, daysBefore = 1)
        val seven = java.time.LocalTime.of(7, 0)
        val evening = java.time.LocalTime.of(20, 0)

        schedulerWithDefault(seven).schedule(item)
        val early = enqueued().single().initialDelayMillis
        schedulerWithDefault(evening).schedule(item)
        val late = enqueued().single().initialDelayMillis

        assertEquals("still exactly one job", 1, enqueued().size)
        val thirteenHours = 13 * 60 * 60 * 1000L
        assertTrue("20:00 is 13h after 07:00 (was ${late - early})", kotlin.math.abs((late - early) - thirteenHours) < 60_000)
        assertEquals(delayMillisFor(item, evening)!!.toDouble(), late.toDouble(), 120_000.0)
    }

    @Test
    fun anItemsOwnTime_winsOverTheDefault() {
        val own = item(expiresInDays = 30, daysBefore = 1).copy(reminderMinuteOfDay = 19 * 60 + 30)

        schedulerWithDefault(java.time.LocalTime.of(7, 0)).schedule(own)

        val expected = delayMillisFor(own, java.time.LocalTime.of(7, 0))!!
        assertEquals(expected.toDouble(), enqueued().single().initialDelayMillis.toDouble(), 120_000.0)
        val other = delayMillisFor(own.copy(reminderMinuteOfDay = null), java.time.LocalTime.of(7, 0))!!
        assertTrue("own time differs from the default's delay", kotlin.math.abs(expected - other) > 60 * 60 * 1000L)
    }

    @Test
    fun rescheduleAll_replacesEachJobOnce_andCancelsTheOnesThatNoLongerApply() {
        val a = item(expiresInDays = 30, daysBefore = 1)
        scheduler.schedule(a)
        val before = enqueued().single()

        scheduler.rescheduleAll(listOf(a))
        scheduler.rescheduleAll(listOf(a))

        assertEquals("no duplicates after repeated rescheduling", 1, enqueued().size)
        assertTrue("replaced", enqueued().single().id != before.id)

        scheduler.rescheduleAll(listOf(a.copy(reminderDaysBefore = null)))
        assertTrue(enqueued().isEmpty())
    }

    @Test
    fun aReminderTimeEarlierToday_isNotScheduled() {
        val now = java.time.LocalTime.now()
        // Due today with "on the day": a time one minute ago has passed, so nothing is enqueued.
        val minuteAgo = (now.hour * 60 + now.minute - 1).coerceAtLeast(0)
        if (now.hour * 60 + now.minute == 0) return // cannot test a past time at 00:00
        scheduler.schedule(item(expiresInDays = 0, daysBefore = 0).copy(reminderMinuteOfDay = minuteAgo))

        assertTrue(enqueued().isEmpty())
    }
}
