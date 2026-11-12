package com.expirybuddy.app.domain.model

/**
 * The choices offered for "how early to remind". Values are `reminderDaysBefore`; null = no
 * reminder. Any other whole number of days up to [MAX_CUSTOM_DAYS] is a "custom" choice.
 */
object ReminderOptions {
    /** Presets shown in the menus (a custom number is entered separately). */
    val PRESETS: List<Int?> = listOf(null, 0, 1, 2, 3)

    /** Pre-selected for new items until the user picks another default in Settings. */
    const val DEFAULT_DAYS_BEFORE = 1

    const val MAX_CUSTOM_DAYS = 365

    fun isPreset(daysBefore: Int?): Boolean = daysBefore in PRESETS

    fun isValidCustom(days: Int?): Boolean = days != null && days in 1..MAX_CUSTOM_DAYS
}
