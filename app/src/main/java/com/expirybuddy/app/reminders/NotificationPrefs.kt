package com.expirybuddy.app.reminders

import android.content.Context

/**
 * Remembers that the notification explanation has been shown, so the user is asked at most once
 * from the Add/Edit flow. After that, Settings is the place to change the decision.
 */
class NotificationPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun hasExplained(): Boolean = prefs.getBoolean(KEY_EXPLAINED, false)

    fun markExplained() {
        prefs.edit().putBoolean(KEY_EXPLAINED, true).apply()
    }

    private companion object {
        const val FILE = "expiry_buddy_prefs"
        const val KEY_EXPLAINED = "notification_explained"
    }
}
