package com.expirybuddy.app.ui.screens.settings

import com.expirybuddy.app.testing.FakeUserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsViewModelTest {
    private val preferences = FakeUserPreferences(defaultReminderDays = 3)

    @Test
    fun startsWithTheSavedDefault() {
        assertEquals(3, SettingsViewModel(preferences).defaultReminderDays.value)
    }

    @Test
    fun changingTheDefault_isSavedAndExposed() {
        val vm = SettingsViewModel(preferences)

        vm.onDefaultReminderChange(7)

        assertEquals(7, vm.defaultReminderDays.value)
        assertEquals(7, preferences.defaultReminderDays)
    }

    @Test
    fun noReminder_isAValidDefault() {
        val vm = SettingsViewModel(preferences)

        vm.onDefaultReminderChange(null)

        assertNull(vm.defaultReminderDays.value)
        assertNull(preferences.defaultReminderDays)
    }
}
