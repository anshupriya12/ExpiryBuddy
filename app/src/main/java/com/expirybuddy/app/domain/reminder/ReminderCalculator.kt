package com.expirybuddy.app.domain.reminder

import com.expirybuddy.app.domain.model.ExpiryItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure reminder rules: no Android, no WorkManager, no system clock (callers pass "now"/"today").
 *
 * A reminder is a calendar date (expiry date minus `reminderDaysBefore`) plus a fixed time of day,
 * [REMINDER_TIME], in the device's current time zone.
 */
object ReminderCalculator {
    /** Local time of day at which reminders are delivered. */
    val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    /**
     * The calendar day the reminder is for, or null if the item has no reminder. An item marked as
     * used never has one, so scheduling it cancels any pending reminder.
     */
    fun reminderDate(item: ExpiryItem): LocalDate? {
        if (item.isUsed) return null
        val daysBefore = item.reminderDaysBefore?.takeIf { it >= 0 } ?: return null
        return item.expiryDate.minusDays(daysBefore.toLong())
    }

    /**
     * How long from [now] until the reminder should fire, or null when nothing should be scheduled:
     * the item has no reminder, or the reminder moment is not strictly in the future.
     *
     * Using a zoned moment (not just a date) means a reminder for today that is still ahead of
     * [REMINDER_TIME] is scheduled, and daylight-saving changes are handled by the time zone rules.
     */
    fun delayUntilReminder(item: ExpiryItem, now: ZonedDateTime): Duration? {
        val date = reminderDate(item) ?: return null
        val trigger = ZonedDateTime.of(date, REMINDER_TIME, now.zone)
        return if (trigger.isAfter(now)) Duration.between(now, trigger) else null
    }

    /**
     * Whether a reminder that has just fired should still be shown: the item has a reminder, the
     * reminder day has arrived, and the item has not passed its expiry date. A reminder delivered
     * late (phone off at 9:00) is still useful until the expiry date is over.
     */
    fun isRelevant(item: ExpiryItem, today: LocalDate): Boolean {
        val date = reminderDate(item) ?: return false
        return !today.isBefore(date) && !today.isAfter(item.expiryDate)
    }
}
