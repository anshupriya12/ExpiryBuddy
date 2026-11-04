package com.expirybuddy.app.domain.expiry

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, UPCOMING }

/**
 * Classifies a calendar expiry date relative to "today". Pure functions on [LocalDate]s: no clock,
 * no time zone and no Android, so every boundary is unit-testable. The caller decides what
 * "today" is (the user's local date).
 */
object ExpiryClassifier {
    /** Items expiring today through this many days ahead are "Expiring Soon". The one place to change it. */
    const val EXPIRING_SOON_DAYS = 7L

    /** Whole calendar days from [today] to [expiryDate]; negative once expired. */
    fun daysUntil(expiryDate: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, expiryDate)

    fun classify(expiryDate: LocalDate, today: LocalDate): ExpiryStatus {
        val days = daysUntil(expiryDate, today)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= EXPIRING_SOON_DAYS -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.UPCOMING
        }
    }
}
