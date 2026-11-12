package com.expirybuddy.app.data.prefs

import android.content.Context
import com.expirybuddy.app.domain.model.ReminderOptions

/**
 * Small local settings that are not item data. No sync, no account: just this device.
 */
interface UserPreferences {
    /** True once the user has finished or skipped onboarding; it is never shown again after that. */
    var onboardingCompleted: Boolean

    /** Reminder pre-selected for new items: days before expiry, or null for "no reminder". */
    var defaultReminderDays: Int?
}

class SharedPreferencesUserPreferences(context: Context) : UserPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    override var defaultReminderDays: Int?
        get() = if (prefs.contains(KEY_DEFAULT_REMINDER)) {
            prefs.getInt(KEY_DEFAULT_REMINDER, NO_REMINDER).takeIf { it != NO_REMINDER }
        } else {
            ReminderOptions.DEFAULT_DAYS_BEFORE
        }
        set(value) = prefs.edit().putInt(KEY_DEFAULT_REMINDER, value ?: NO_REMINDER).apply()

    private companion object {
        // Same file as NotificationPrefs, which uses a different key.
        const val FILE = "expiry_buddy_prefs"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
        const val KEY_DEFAULT_REMINDER = "default_reminder_days"
        const val NO_REMINDER = -1
    }
}
