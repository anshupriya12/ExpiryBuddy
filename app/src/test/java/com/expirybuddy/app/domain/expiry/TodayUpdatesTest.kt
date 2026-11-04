package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.testing.VirtualTimeClock
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TodayUpdatesTest {

    @Test
    fun emitsTodayThenTomorrowAfterMidnight() = runTest {
        val clock = VirtualTimeClock(testScheduler, Instant.parse("2026-03-01T23:59:30Z"))

        val days = clock.todayUpdates().take(2).toList()

        assertEquals(listOf(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)), days)
    }

    @Test
    fun midnightIsLocalNotUtc() = runTest {
        // 18:29:30 UTC is 23:59:30 in India (UTC+5:30), so the date flips 30 seconds later.
        val clock = VirtualTimeClock(
            testScheduler,
            Instant.parse("2026-03-01T18:29:30Z"),
            ZoneId.of("Asia/Kolkata"),
        )

        val days = clock.todayUpdates().take(2).toList()

        assertEquals(listOf(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)), days)
    }

    @Test
    fun emitsConsecutiveDaysAcrossAMonthEnd() = runTest {
        val clock = VirtualTimeClock(testScheduler, Instant.parse("2026-02-28T12:00:00Z"))

        val days = clock.todayUpdates().take(3).toList()

        assertEquals(
            listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)),
            days,
        )
    }
}
