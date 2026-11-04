package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.expiry.ExpiryStatus.EXPIRED
import com.expirybuddy.app.domain.expiry.ExpiryStatus.EXPIRING_SOON
import com.expirybuddy.app.domain.expiry.ExpiryStatus.UPCOMING
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpiryClassifierTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun classify(daysFromToday: Long) =
        ExpiryClassifier.classify(today.plusDays(daysFromToday), today)

    @Test
    fun threshold_isSevenDays() {
        assertEquals(7L, ExpiryClassifier.EXPIRING_SOON_DAYS)
    }

    @Test
    fun yesterday_isExpired() = assertEquals(EXPIRED, classify(-1))

    @Test
    fun today_isExpiringSoon_notExpired() = assertEquals(EXPIRING_SOON, classify(0))

    @Test
    fun tomorrow_isExpiringSoon() = assertEquals(EXPIRING_SOON, classify(1))

    @Test
    fun sevenDaysAway_isStillExpiringSoon() = assertEquals(EXPIRING_SOON, classify(7))

    @Test
    fun eightDaysAway_isUpcoming() = assertEquals(UPCOMING, classify(8))

    @Test
    fun farPast_isExpired() = assertEquals(EXPIRED, classify(-10_000))

    @Test
    fun farFuture_isUpcoming() = assertEquals(UPCOMING, classify(10_000))

    @Test
    fun thresholdBoundary_followsTheConstant() {
        val limit = ExpiryClassifier.EXPIRING_SOON_DAYS
        assertEquals(EXPIRING_SOON, classify(limit))
        assertEquals(UPCOMING, classify(limit + 1))
    }

    @Test
    fun boundaries_workAcrossMonthEnd() {
        val endOfMonth = LocalDate.of(2026, 1, 28)
        assertEquals(EXPIRING_SOON, ExpiryClassifier.classify(LocalDate.of(2026, 2, 4), endOfMonth))
        assertEquals(UPCOMING, ExpiryClassifier.classify(LocalDate.of(2026, 2, 5), endOfMonth))
    }

    @Test
    fun boundaries_workAcrossYearEnd() {
        val newYearsEve = LocalDate.of(2026, 12, 31)
        assertEquals(EXPIRED, ExpiryClassifier.classify(LocalDate.of(2026, 12, 30), newYearsEve))
        assertEquals(EXPIRING_SOON, ExpiryClassifier.classify(LocalDate.of(2027, 1, 7), newYearsEve))
        assertEquals(UPCOMING, ExpiryClassifier.classify(LocalDate.of(2027, 1, 8), newYearsEve))
    }

    @Test
    fun boundaries_workAcrossLeapDay() {
        val feb27 = LocalDate.of(2028, 2, 27)
        assertEquals(EXPIRING_SOON, ExpiryClassifier.classify(LocalDate.of(2028, 3, 5), feb27))
        assertEquals(UPCOMING, ExpiryClassifier.classify(LocalDate.of(2028, 3, 6), feb27))
        assertEquals(2L, ExpiryClassifier.daysUntil(LocalDate.of(2028, 2, 29), feb27))
        // In a non-leap year the same dates are one day closer together.
        assertEquals(2L, ExpiryClassifier.daysUntil(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 2, 27)))
    }

    @Test
    fun daysUntil_isSignedCalendarDays() {
        assertEquals(0L, ExpiryClassifier.daysUntil(today, today))
        assertEquals(5L, ExpiryClassifier.daysUntil(today.plusDays(5), today))
        assertEquals(-3L, ExpiryClassifier.daysUntil(today.minusDays(3), today))
    }
}
