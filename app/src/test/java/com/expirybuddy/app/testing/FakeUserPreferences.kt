package com.expirybuddy.app.testing

import com.expirybuddy.app.data.prefs.UserPreferences

class FakeUserPreferences(
    override var onboardingCompleted: Boolean = false,
    override var defaultReminderDays: Int? = 3,
    override var defaultReminderMinutes: Int = 9 * 60,
) : UserPreferences
