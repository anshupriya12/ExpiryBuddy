package com.expirybuddy.app.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expirybuddy.app.data.prefs.UserPreferences
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.reminders.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the notification defaults shown in Settings. The notification permission state is read from
 * the OS by the screen.
 *
 * Changing the default *time* re-plans every pending reminder that follows the default, so it takes
 * effect immediately. Changing the default *advance notice* only applies to items added afterwards.
 */
class SettingsViewModel(
    private val preferences: UserPreferences,
    private val repository: ExpiryItemRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _defaultReminderDays = MutableStateFlow(preferences.defaultReminderDays)

    /** Days before the use-by date that new items start with, or null for "no reminder". */
    val defaultReminderDays: StateFlow<Int?> = _defaultReminderDays.asStateFlow()

    private val _defaultReminderMinutes = MutableStateFlow(preferences.defaultReminderMinutes)

    /** Default notification time, minutes after midnight. */
    val defaultReminderMinutes: StateFlow<Int> = _defaultReminderMinutes.asStateFlow()

    fun onDefaultReminderChange(daysBefore: Int?) {
        preferences.defaultReminderDays = daysBefore
        _defaultReminderDays.value = daysBefore
    }

    fun onDefaultTimeChange(minuteOfDay: Int) {
        if (minuteOfDay == preferences.defaultReminderMinutes) return
        preferences.defaultReminderMinutes = minuteOfDay
        _defaultReminderMinutes.value = preferences.defaultReminderMinutes
        viewModelScope.launch {
            try {
                scheduler.rescheduleAll(repository.getAllItems())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "Could not re-plan reminders", e)
            }
        }
    }
}
