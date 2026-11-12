package com.expirybuddy.app.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The real SharedPreferences-backed settings: the 1-day default, and saved choices never being overwritten. */
@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsFile = context.getSharedPreferences("expiry_buddy_prefs", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        prefsFile.edit().clear().commit()
    }

    @After
    fun clearAgain() {
        prefsFile.edit().clear().commit()
    }

    @Test
    fun aFreshInstall_defaultsToOneDayBefore_at9am() {
        val prefs = SharedPreferencesUserPreferences(context)
        assertEquals(1, prefs.defaultReminderDays)
        assertEquals(9 * 60, prefs.defaultReminderMinutes)
    }

    @Test
    fun anExplicitlySavedDefault_isPreserved_includingThreeDaysAndNoReminder() {
        prefsFile.edit().putInt("default_reminder_days", 3).commit()
        assertEquals(3, SharedPreferencesUserPreferences(context).defaultReminderDays)

        prefsFile.edit().putInt("default_reminder_days", -1).commit()
        assertNull(SharedPreferencesUserPreferences(context).defaultReminderDays)
    }

    @Test
    fun savingADefault_roundTrips_andTheTimeIsIndependent() {
        val prefs = SharedPreferencesUserPreferences(context)
        prefs.defaultReminderDays = 2
        prefs.defaultReminderMinutes = 19 * 60 + 30

        val again = SharedPreferencesUserPreferences(context)
        assertEquals(2, again.defaultReminderDays)
        assertEquals(19 * 60 + 30, again.defaultReminderMinutes)
    }
}
