package com.expirybuddy.app.domain.model

/** The reminder choices offered in the form. Values are `reminderDaysBefore`; null = no reminder. */
object ReminderOptions {
    val ALL: List<Int?> = listOf(null, 0, 1, 3, 7, 14)

    /** Pre-selected for new items. */
    const val DEFAULT_DAYS_BEFORE = 3
}
