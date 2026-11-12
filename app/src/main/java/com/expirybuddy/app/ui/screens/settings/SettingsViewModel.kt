package com.expirybuddy.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.expirybuddy.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the reminder default shown in Settings; the notification permission state is read from the OS. */
class SettingsViewModel(private val preferences: UserPreferences) : ViewModel() {

    private val _defaultReminderDays = MutableStateFlow(preferences.defaultReminderDays)

    /** Days before expiry that new items start with, or null for "no reminder". */
    val defaultReminderDays: StateFlow<Int?> = _defaultReminderDays.asStateFlow()

    fun onDefaultReminderChange(daysBefore: Int?) {
        preferences.defaultReminderDays = daysBefore
        _defaultReminderDays.value = daysBefore
    }
}
