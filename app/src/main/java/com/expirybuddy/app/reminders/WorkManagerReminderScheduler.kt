package com.expirybuddy.app.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules reminders as one-time WorkManager jobs with an initial delay. WorkManager is
 * battery-friendly and survives reboots, but delivery is approximate: the system may run the job
 * a little late in Doze. (Exact alarms are deliberately not used.)
 *
 * WorkManager calls here are asynchronous, so these functions are safe to call from any thread.
 */
class WorkManagerReminderScheduler(
    private val workManager: WorkManager,
    private val clock: Clock,
    /** The user's default notification time, read each time so a change in Settings applies at once. */
    private val defaultTime: () -> LocalTime = { ReminderCalculator.DEFAULT_REMINDER_TIME },
) : ReminderScheduler {

    override fun schedule(item: ExpiryItem) {
        val delay = ReminderCalculator.delayUntilReminder(item, ZonedDateTime.now(clock), defaultTime())
        if (delay == null) {
            cancel(item.id)
        } else {
            enqueue(item.id, delay)
        }
    }

    override fun cancel(itemId: Long) {
        workManager.cancelUniqueWork(ReminderWork.uniqueName(itemId))
    }

    override fun restore(items: List<ExpiryItem>) {
        val now = ZonedDateTime.now(clock)
        val time = defaultTime()
        items.forEach { item ->
            ReminderCalculator.delayUntilReminder(item, now, time)?.let { enqueue(item.id, it) }
        }
    }

    override fun rescheduleAll(items: List<ExpiryItem>) {
        items.forEach(::schedule)
    }

    private fun enqueue(itemId: Long, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to itemId))
            .build()
        // REPLACE: editing an item swaps its old reminder for the new one.
        workManager.enqueueUniqueWork(ReminderWork.uniqueName(itemId), ExistingWorkPolicy.REPLACE, request)
    }
}
