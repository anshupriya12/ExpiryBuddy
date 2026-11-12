package com.expirybuddy.app.testing

import com.expirybuddy.app.data.prefs.UserPreferences
import com.expirybuddy.app.domain.model.ReminderOptions

class FakeUserPreferences(
    override var onboardingCompleted: Boolean = false,
    override var defaultReminderDays: Int? = ReminderOptions.DEFAULT_DAYS_BEFORE,
    override var defaultReminderMinutes: Int = 9 * 60,
) : UserPreferences
