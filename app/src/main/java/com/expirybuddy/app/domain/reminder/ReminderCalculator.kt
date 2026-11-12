package com.expirybuddy.app.domain.reminder

import com.expirybuddy.app.domain.model.ExpiryItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure reminder rules: no Android, no WorkManager, no system clock (callers pass "now"/"today").
 *
 * A reminder is a calendar date (use-by date minus `reminderDaysBefore`) plus a time of day in the
 * device's current time zone. The time of day is the item's own time if it has one, otherwise the
 * default notification time from Settings.
 */
object ReminderCalculator {
    /** Delivery time used until the user chooses another one in Settings (the pre-V2.2 behaviour). */
    val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    const val MINUTES_PER_DAY = 24 * 60

    fun timeOfMinutes(minuteOfDay: Int): LocalTime {
        val m = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        return LocalTime.of(m / 60, m % 60)
    }

    fun minutesOf(time: LocalTime): Int = time.hour * 60 + time.minute

    /** The time of day this item is reminded at: its own override, else [defaultTime]. */
    fun reminderTime(item: ExpiryItem, defaultTime: LocalTime = DEFAULT_REMINDER_TIME): LocalTime =
        item.reminderMinuteOfDay?.let(::timeOfMinutes) ?: defaultTime

    /**
     * The calendar day the reminder is for, or null if the item has no reminder. An item marked as
     * used never has one, so scheduling it cancels any pending reminder.
     */
    fun reminderDate(item: ExpiryItem): LocalDate? {
        if (item.isUsed) return null
        val daysBefore = item.reminderDaysBefore?.takeIf { it >= 0 } ?: return null
        return item.expiryDate.minusDays(daysBefore.toLong())
    }

    /** The planned moment of the reminder in [zone], or null when the item has no reminder. */
    fun reminderMoment(item: ExpiryItem, defaultTime: LocalTime, zone: ZoneId): ZonedDateTime? {
        val date = reminderDate(item) ?: return null
        return ZonedDateTime.of(date, reminderTime(item, defaultTime), zone)
    }

    /**
     * How long from [now] until the reminder should fire, or null when nothing should be scheduled:
     * the item has no reminder, or the reminder moment is not strictly in the future (for example it
     * is today and the chosen time has already passed, or the deadline is in the past).
     *
     * Using a zoned moment (not just a date) means a reminder for today that is still ahead of its
     * time is scheduled, and daylight-saving changes are handled by the time zone rules.
     */
    fun delayUntilReminder(
        item: ExpiryItem,
        now: ZonedDateTime,
        defaultTime: LocalTime = DEFAULT_REMINDER_TIME,
    ): Duration? {
        val trigger = reminderMoment(item, defaultTime, now.zone) ?: return null
        return if (trigger.isAfter(now)) Duration.between(now, trigger) else null
    }

    /** What saving a reminder configuration would do right now; used for the form's one-line summary. */
    sealed interface Plan {
        data object None : Plan

        /** A notification will be scheduled for [moment]. */
        data class Scheduled(val moment: ZonedDateTime) : Plan

        /** The reminder moment is not in the future, so no notification will be sent. */
        data class AlreadyPassed(val moment: ZonedDateTime) : Plan
    }

    fun plan(
        useByDate: LocalDate,
        daysBefore: Int?,
        minuteOfDay: Int?,
        defaultTime: LocalTime,
        now: ZonedDateTime,
    ): Plan {
        if (daysBefore == null || daysBefore < 0) return Plan.None
        val time = minuteOfDay?.let(::timeOfMinutes) ?: defaultTime
        val moment = ZonedDateTime.of(useByDate.minusDays(daysBefore.toLong()), time, now.zone)
        return if (moment.isAfter(now)) Plan.Scheduled(moment) else Plan.AlreadyPassed(moment)
    }

    /**
     * Whether a reminder that has just fired should still be shown: the item has a reminder, the
     * reminder day has arrived, and the item has not passed its use-by date. A reminder delivered
     * late (phone off at the chosen time) is still useful until the use-by date is over.
     */
    fun isRelevant(item: ExpiryItem, today: LocalDate): Boolean {
        val date = reminderDate(item) ?: return false
        return !today.isBefore(date) && !today.isAfter(item.expiryDate)
    }
}
