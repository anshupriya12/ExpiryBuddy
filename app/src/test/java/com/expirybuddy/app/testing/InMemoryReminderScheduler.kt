package com.expirybuddy.app.testing

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import com.expirybuddy.app.reminders.ReminderScheduler
import com.expirybuddy.app.reminders.ReminderWork
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Mirrors the WorkManager scheduler's contract in memory (unique name per item, replace on
 * schedule, cancel when nothing applies) using the real [ReminderCalculator], so scheduling
 * behaviour can be asserted in plain JVM tests.
 */
class InMemoryReminderScheduler(private val clock: Clock) : ReminderScheduler {
    /** Pending reminders: unique work name to delay from "now". */
    val pending = linkedMapOf<String, Duration>()
    val calls = mutableListOf<String>()
    var failWith: Exception? = null

    override fun schedule(item: ExpiryItem) {
        calls += "schedule:${item.id}"
        failWith?.let { throw it }
        val name = ReminderWork.uniqueName(item.id)
        val delay = ReminderCalculator.delayUntilReminder(item, ZonedDateTime.now(clock))
        if (delay == null) pending.remove(name) else pending[name] = delay
    }

    override fun cancel(itemId: Long) {
        calls += "cancel:$itemId"
        failWith?.let { throw it }
        pending.remove(ReminderWork.uniqueName(itemId))
    }

    override fun restore(items: List<ExpiryItem>) {
        calls += "restore"
        val now = ZonedDateTime.now(clock)
        items.forEach { item ->
            ReminderCalculator.delayUntilReminder(item, now)?.let { pending[ReminderWork.uniqueName(item.id)] = it }
        }
    }
}
