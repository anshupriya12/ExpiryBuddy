package com.expirybuddy.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class DatePickerMillisTest {

    @Test
    fun utcMidnight_mapsToThatCalendarDate() {
        // 2026-03-12T00:00:00Z
        assertEquals(LocalDate.of(2026, 3, 12), DatePickerMillis.toLocalDate(1_773_273_600_000L))
    }

    @Test
    fun roundTrip_isLossless() {
        listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2026, 12, 31), LocalDate.of(1999, 1, 1))
            .forEach { assertEquals(it, DatePickerMillis.toLocalDate(DatePickerMillis.fromLocalDate(it))) }
    }

    @Test
    fun conversion_ignoresDeviceTimeZone() {
        val original = TimeZone.getDefault()
        try {
            val date = LocalDate.of(2026, 3, 12)
            listOf("Pacific/Kiritimati", "America/Los_Angeles", "Asia/Kolkata", "UTC").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val millis = DatePickerMillis.fromLocalDate(date)
                assertEquals("zone $zone", date, DatePickerMillis.toLocalDate(millis))
                assertEquals("zone $zone", 1_773_273_600_000L, millis)
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
