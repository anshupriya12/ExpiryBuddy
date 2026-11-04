package com.expirybuddy.app.domain.reminder

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderCalculatorTest {
    private val utc = ZoneId.of("UTC")

    private fun item(expiry: LocalDate, daysBefore: Int?) = ExpiryItem(
        id = 1,
        name = "Milk",
        category = ItemCategory.FOOD,
        expiryDate = expiry,
        reminderDaysBefore = daysBefore,
        createdAt = java.time.Instant.EPOCH,
    )

    private fun at(date: String, time: String, zone: ZoneId = utc): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.parse("${date}T$time"), zone)

    // --- reminder date -------------------------------------------------------------------------

    @Test
    fun reminderDate_isExpiryMinusDaysBefore() {
        assertEquals(LocalDate.of(2026, 3, 7), ReminderCalculator.reminderDate(item(LocalDate.of(2026, 3, 10), 3)))
    }

    @Test
    fun reminderDate_zeroDaysBefore_isTheExpiryDay() {
        val expiry = LocalDate.of(2026, 3, 10)
        assertEquals(expiry, ReminderCalculator.reminderDate(item(expiry, 0)))
    }

    @Test
    fun reminderDate_noReminder_isNull() {
        assertNull(ReminderCalculator.reminderDate(item(LocalDate.of(2026, 3, 10), null)))
    }

    @Test
    fun reminderDate_negativeDays_isTreatedAsNoReminder() {
        assertNull(ReminderCalculator.reminderDate(item(LocalDate.of(2026, 3, 10), -2)))
    }

    @Test
    fun reminderDate_crossesMonthYearAndLeapDayBoundaries() {
        assertEquals(LocalDate.of(2026, 2, 28), ReminderCalculator.reminderDate(item(LocalDate.of(2026, 3, 3), 3)))
        assertEquals(LocalDate.of(2026, 12, 31), ReminderCalculator.reminderDate(item(LocalDate.of(2027, 1, 7), 7)))
        assertEquals(LocalDate.of(2028, 2, 29), ReminderCalculator.reminderDate(item(LocalDate.of(2028, 3, 1), 1)))
        assertEquals(LocalDate.of(2027, 2, 28), ReminderCalculator.reminderDate(item(LocalDate.of(2027, 3, 1), 1)))
    }

    // --- delay until reminder (schedule only if in the future) -----------------------------------

    @Test
    fun futureReminder_hasExactDelayToNineAm() {
        val result = ReminderCalculator.delayUntilReminder(
            item(LocalDate.of(2026, 3, 10), 3), // reminder 7 March, 09:00
            at("2026-03-01", "08:00"),
        )
        assertEquals(Duration.ofDays(6).plusHours(1), result)
    }

    @Test
    fun reminderDayIsToday_butNineAmNotReached_isScheduled() {
        val result = ReminderCalculator.delayUntilReminder(
            item(LocalDate.of(2026, 3, 10), 3),
            at("2026-03-07", "06:30"),
        )
        assertEquals(Duration.ofMinutes(150), result)
    }

    @Test
    fun reminderDayIsToday_andNineAmPassed_isNotScheduled() {
        assertNull(
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), 3), at("2026-03-07", "09:01")),
        )
    }

    @Test
    fun reminderExactlyNow_isNotInTheFuture() {
        assertNull(
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), 3), at("2026-03-07", "09:00")),
        )
    }

    @Test
    fun oneSecondBeforeReminder_isScheduled() {
        assertEquals(
            Duration.ofSeconds(1),
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), 3), at("2026-03-07", "08:59:59")),
        )
    }

    @Test
    fun reminderDateInThePast_isNotScheduled() {
        assertNull(
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), 7), at("2026-03-05", "12:00")),
        )
    }

    @Test
    fun alreadyExpiredItem_isNotScheduled() {
        assertNull(
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 2, 1), 0), at("2026-03-01", "08:00")),
        )
    }

    @Test
    fun noReminder_isNotScheduled() {
        assertNull(
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), null), at("2026-03-01", "08:00")),
        )
    }

    @Test
    fun delay_usesTheDeviceTimeZoneForNineAm() {
        // 03:00 UTC is 08:30 in India (UTC+5:30): 9:00 IST is only 30 minutes away.
        val kolkata = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.of(LocalDateTime.parse("2026-03-07T03:00:00"), utc).withZoneSameInstant(kolkata)
        assertEquals(
            Duration.ofMinutes(30),
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 3, 10), 3), now),
        )
    }

    @Test
    fun delay_isCorrectAcrossASpringForwardDstChange() {
        // New York clocks jump forward on 8 March 2026. From 20:00 on the 7th (EST) to 09:00 on the
        // 8th (EDT) is only 12 hours of real time, not 13.
        val newYork = ZoneId.of("America/New_York")
        val result = ReminderCalculator.delayUntilReminder(
            item(LocalDate.of(2026, 3, 9), 1), // reminder 8 March
            at("2026-03-07", "20:00", newYork),
        )
        assertEquals(Duration.ofHours(12), result)
    }

    @Test
    fun delay_isCorrectAcrossAFallBackDstChange() {
        // Clocks go back on 1 November 2026: 20:00 on the 31st (EDT) to 09:00 on the 1st (EST) is 14 hours.
        val newYork = ZoneId.of("America/New_York")
        val result = ReminderCalculator.delayUntilReminder(
            item(LocalDate.of(2026, 11, 2), 1), // reminder 1 November
            at("2026-10-31", "20:00", newYork),
        )
        assertEquals(Duration.ofHours(14), result)
    }

    // --- relevance when the reminder fires ---------------------------------------------------------

    @Test
    fun relevant_onTheReminderDay() {
        assertTrue(ReminderCalculator.isRelevant(item(LocalDate.of(2026, 3, 10), 3), LocalDate.of(2026, 3, 7)))
    }

    @Test
    fun relevant_betweenReminderDayAndExpiryDayInclusive() {
        val item = item(LocalDate.of(2026, 3, 10), 3)
        assertTrue(ReminderCalculator.isRelevant(item, LocalDate.of(2026, 3, 8)))
        assertTrue(ReminderCalculator.isRelevant(item, LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun notRelevant_afterTheExpiryDate() {
        assertFalse(ReminderCalculator.isRelevant(item(LocalDate.of(2026, 3, 10), 3), LocalDate.of(2026, 3, 11)))
    }

    @Test
    fun notRelevant_beforeTheReminderDay() {
        assertFalse(ReminderCalculator.isRelevant(item(LocalDate.of(2026, 3, 10), 3), LocalDate.of(2026, 3, 6)))
    }

    @Test
    fun notRelevant_whenThereIsNoReminder() {
        assertFalse(ReminderCalculator.isRelevant(item(LocalDate.of(2026, 3, 10), null), LocalDate.of(2026, 3, 9)))
    }

    @Test
    fun reminderTime_isNineAm() {
        assertEquals(java.time.LocalTime.of(9, 0), ReminderCalculator.REMINDER_TIME)
    }
}
