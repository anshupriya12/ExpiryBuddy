package com.expirybuddy.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material's DatePicker speaks in "UTC midnight" milliseconds. Converting with the device zone
 * instead of UTC is the classic off-by-one-day bug, so both directions use UTC explicitly.
 */
object DatePickerMillis {
    fun toLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun fromLocalDate(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
