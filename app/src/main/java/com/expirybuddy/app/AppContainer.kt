package com.expirybuddy.app

import android.content.Context
import androidx.work.WorkManager
import com.expirybuddy.app.data.local.ExpiryBuddyDatabase
import com.expirybuddy.app.data.photo.FilePhotoStorage
import com.expirybuddy.app.data.photo.PhotoStorage
import com.expirybuddy.app.data.prefs.SharedPreferencesUserPreferences
import com.expirybuddy.app.data.prefs.UserPreferences
import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.data.repository.ReminderSyncingRepository
import com.expirybuddy.app.data.repository.RoomExpiryItemRepository
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import com.expirybuddy.app.reminders.NotificationPrefs
import com.expirybuddy.app.reminders.ReminderScheduler
import com.expirybuddy.app.reminders.WorkManagerReminderScheduler
import java.time.Clock

/**
 * Hand-rolled dependency holder, created once by [ExpiryBuddyApp]. For an app this size a DI
 * framework would add more code than it removes. Heavy objects are created lazily on first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: ExpiryBuddyDatabase by lazy { ExpiryBuddyDatabase.create(appContext) }

    /** Injected so time-dependent logic can be tested with a fixed clock. */
    val clock: Clock = Clock.systemDefaultZone()

    val reminderScheduler: ReminderScheduler by lazy {
        WorkManagerReminderScheduler(WorkManager.getInstance(appContext), clock) {
            ReminderCalculator.timeOfMinutes(userPreferences.defaultReminderMinutes)
        }
    }

    /** Saves and deletes also keep reminders in sync (see [ReminderSyncingRepository]). */
    val itemRepository: ExpiryItemRepository by lazy {
        ReminderSyncingRepository(RoomExpiryItemRepository(database.itemDao()), reminderScheduler)
    }

    val notificationPrefs: NotificationPrefs by lazy { NotificationPrefs(appContext) }

    val userPreferences: UserPreferences by lazy { SharedPreferencesUserPreferences(appContext) }

    val photoStorage: PhotoStorage by lazy { FilePhotoStorage(appContext) }
}
