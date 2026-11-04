package com.expirybuddy.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun roundTrip_preservesDate() {
        val date = LocalDate.of(2026, 3, 12)
        assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
    }

    @Test
    fun roundTrip_handlesLeapDayAndYearBoundaries() {
        listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(1970, 1, 1),
            LocalDate.of(1969, 12, 31),
        ).forEach { date ->
            assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
        }
    }

    @Test
    fun epochDay_isZeroOnUnixEpoch() {
        assertEquals(0L, converters.localDateToEpochDay(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun nulls_passThrough() {
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
    }

    @Test
    fun storedValue_isIndependentOfDeviceTimeZone() {
        val original = TimeZone.getDefault()
        try {
            val date = LocalDate.of(2026, 6, 15)
            val stored = mutableSetOf<Long?>()
            listOf("Pacific/Kiritimati", "UTC", "Pacific/Pago_Pago").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                stored += converters.localDateToEpochDay(date)
            }
            assertEquals(1, stored.size)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun epochDay_ordersChronologically() {
        val earlier = converters.localDateToEpochDay(LocalDate.of(2026, 1, 31))!!
        val later = converters.localDateToEpochDay(LocalDate.of(2026, 2, 1))!!
        assert(earlier < later)
    }
}
