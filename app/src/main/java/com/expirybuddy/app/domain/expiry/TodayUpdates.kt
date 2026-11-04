package com.expirybuddy.app.domain.expiry

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Emits today's date now and again each time local midnight passes, so statuses stay correct if
 * the app is left open overnight. Delays can drift while the device sleeps, so the UI also
 * restarts this flow when the screen resumes.
 */
fun Clock.todayUpdates(): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(this@todayUpdates)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
    }
}
