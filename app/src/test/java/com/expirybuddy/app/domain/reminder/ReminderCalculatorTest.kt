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

    // --- items marked as used ------------------------------------------------------------------

    private fun usedItem(expiry: LocalDate, daysBefore: Int?) =
        item(expiry, daysBefore).copy(usedAt = java.time.Instant.parse("2026-03-01T00:00:00Z"))

    @Test
    fun usedItem_hasNoReminderDate_noDelay_andIsNeverRelevant() {
        val used = usedItem(LocalDate.of(2026, 3, 10), 3)

        assertNull(ReminderCalculator.reminderDate(used))
        assertNull(ReminderCalculator.delayUntilReminder(used, at("2026-03-01", "08:00")))
        assertFalse(ReminderCalculator.isRelevant(used, LocalDate.of(2026, 3, 8)))
    }

    @Test
    fun restoredItem_hasItsReminderAgain() {
        val restored = usedItem(LocalDate.of(2026, 3, 10), 3).copy(usedAt = null)

        assertEquals(LocalDate.of(2026, 3, 7), ReminderCalculator.reminderDate(restored))
    }

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
    fun defaultReminderTime_isNineAm() {
        assertEquals(java.time.LocalTime.of(9, 0), ReminderCalculator.DEFAULT_REMINDER_TIME)
    }

    // --- V2.2: configurable time of day ------------------------------------------------------

    private fun hm(h: Int, m: Int) = java.time.LocalTime.of(h, m)

    @Test
    fun specExample_useBy14Nov_oneDayBefore_at1930_remindsOn13Nov_at1930() {
        val moment = ReminderCalculator.reminderMoment(item(LocalDate.of(2026, 11, 14), 1), hm(19, 30), utc)
        assertEquals(at("2026-11-13", "19:30"), moment)
        assertEquals(
            Duration.ofHours(19).plusMinutes(30),
            ReminderCalculator.delayUntilReminder(item(LocalDate.of(2026, 11, 14), 1), at("2026-11-13", "00:00"), hm(19, 30)),
        )
    }

    @Test
    fun itemWithoutOwnTime_followsTheDefaultTime() {
        val it = item(LocalDate.of(2026, 3, 10), 1)
        assertEquals(hm(7, 0), ReminderCalculator.reminderTime(it, hm(7, 0)))
        assertEquals(hm(20, 0), ReminderCalculator.reminderTime(it, hm(20, 0)))
    }

    @Test
    fun itemOwnTime_overridesTheDefault() {
        val paneer = item(LocalDate.of(2026, 3, 10), 0).copy(reminderMinuteOfDay = 19 * 60)
        assertEquals(hm(19, 0), ReminderCalculator.reminderTime(paneer, hm(8, 0)))
        assertEquals(at("2026-03-10", "19:00"), ReminderCalculator.reminderMoment(paneer, hm(8, 0), utc))
    }

    @Test
    fun perItemOverrides_giveDifferentMomentsFromTheSameDefaults() {
        val default = item(LocalDate.of(2026, 3, 10), 1)
        val paneer = item(LocalDate.of(2026, 3, 10), 0).copy(reminderMinuteOfDay = 19 * 60)
        val medicine = item(LocalDate.of(2026, 3, 10), 2).copy(reminderMinuteOfDay = 9 * 60)
        val defaultTime = hm(8, 0)
        assertEquals(at("2026-03-09", "08:00"), ReminderCalculator.reminderMoment(default, defaultTime, utc))
        assertEquals(at("2026-03-10", "19:00"), ReminderCalculator.reminderMoment(paneer, defaultTime, utc))
        assertEquals(at("2026-03-08", "09:00"), ReminderCalculator.reminderMoment(medicine, defaultTime, utc))
    }

    @Test
    fun advanceNotice_onTheDay_1_2_3_andCustomDays() {
        val useBy = LocalDate.of(2026, 3, 20)
        listOf(0, 1, 2, 3, 10, 45).forEach { days ->
            assertEquals(useBy.minusDays(days.toLong()), ReminderCalculator.reminderDate(item(useBy, days)))
        }
    }

    @Test
    fun anyValidTimeIsAccepted_includingMidnightAndLastMinute() {
        val midnight = item(LocalDate.of(2026, 3, 10), 0).copy(reminderMinuteOfDay = 0)
        val late = item(LocalDate.of(2026, 3, 10), 0).copy(reminderMinuteOfDay = 23 * 60 + 59)
        assertEquals(hm(0, 0), ReminderCalculator.reminderTime(midnight))
        assertEquals(hm(23, 59), ReminderCalculator.reminderTime(late))
    }

    @Test
    fun timeConversion_roundTrips_andClampsOutOfRangeValues() {
        assertEquals(hm(7, 30), ReminderCalculator.timeOfMinutes(7 * 60 + 30))
        assertEquals(7 * 60 + 30, ReminderCalculator.minutesOf(hm(7, 30)))
        assertEquals(hm(23, 59), ReminderCalculator.timeOfMinutes(5000))
        assertEquals(hm(0, 0), ReminderCalculator.timeOfMinutes(-4))
    }

    @Test
    fun reminderToday_stillAhead_isScheduled() {
        val it = item(LocalDate.of(2026, 3, 10), 0)
        assertEquals(
            Duration.ofHours(4),
            ReminderCalculator.delayUntilReminder(it, at("2026-03-10", "14:00"), hm(18, 0)),
        )
    }

    @Test
    fun reminderToday_timeAlreadyPassed_isNotScheduled() {
        val it = item(LocalDate.of(2026, 3, 10), 0)
        assertNull(ReminderCalculator.delayUntilReminder(it, at("2026-03-10", "18:01"), hm(18, 0)))
        assertNull(ReminderCalculator.delayUntilReminder(it, at("2026-03-10", "18:00"), hm(18, 0)))
    }

    @Test
    fun deadlineInThePast_isNotScheduled_evenWithAReminder() {
        val it = item(LocalDate.of(2026, 3, 1), 0)
        assertNull(ReminderCalculator.delayUntilReminder(it, at("2026-03-05", "06:00"), hm(7, 0)))
    }

    @Test
    fun deadlineToday_withTimeStillAhead_isScheduled_andWithNoticeDaysIsNot() {
        val today = LocalDate.of(2026, 3, 10)
        assertEquals(
            Duration.ofHours(2),
            ReminderCalculator.delayUntilReminder(item(today, 0), at("2026-03-10", "07:00"), hm(9, 0)),
        )
        // "1 day before" for something due today means yesterday: gone.
        assertNull(ReminderCalculator.delayUntilReminder(item(today, 1), at("2026-03-10", "07:00"), hm(9, 0)))
    }

    @Test
    fun changingTheDefaultTime_movesTheReminder_butNotAnItemsOwnTime() {
        val follower = item(LocalDate.of(2026, 3, 10), 1)
        val own = follower.copy(reminderMinuteOfDay = 6 * 60)
        val now = at("2026-03-08", "00:00")
        assertEquals(Duration.ofHours(33), ReminderCalculator.delayUntilReminder(follower, now, hm(9, 0)))
        assertEquals(Duration.ofHours(44), ReminderCalculator.delayUntilReminder(follower, now, hm(20, 0)))
        assertEquals(Duration.ofHours(30), ReminderCalculator.delayUntilReminder(own, now, hm(9, 0)))
        assertEquals(Duration.ofHours(30), ReminderCalculator.delayUntilReminder(own, now, hm(20, 0)))
    }

    @Test
    fun usedItem_neverHasAReminder_whateverTheTime() {
        val used = usedItem(LocalDate.of(2026, 3, 10), 1).copy(reminderMinuteOfDay = 18 * 60)
        assertNull(ReminderCalculator.reminderMoment(used, hm(9, 0), utc))
    }

    // --- Plan (the form's summary) -----------------------------------------------------------

    @Test
    fun plan_none_scheduled_andAlreadyPassed() {
        val useBy = LocalDate.of(2026, 3, 10)
        val now = at("2026-03-09", "10:00")
        assertEquals(ReminderCalculator.Plan.None, ReminderCalculator.plan(useBy, null, null, hm(9, 0), now))
        assertEquals(
            ReminderCalculator.Plan.Scheduled(at("2026-03-10", "09:00")),
            ReminderCalculator.plan(useBy, 0, null, hm(9, 0), now),
        )
        assertEquals(
            ReminderCalculator.Plan.AlreadyPassed(at("2026-03-09", "09:00")),
            ReminderCalculator.plan(useBy, 1, null, hm(9, 0), now),
        )
    }
}
