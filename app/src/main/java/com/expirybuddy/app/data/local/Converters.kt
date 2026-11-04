package com.expirybuddy.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Stores [LocalDate] as its epoch day (days since 1970-01-01).
 *
 * An epoch day involves no time zone or clock, so a saved expiry date can never shift when the
 * device zone changes, and the integer sorts chronologically in SQL.
 */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
