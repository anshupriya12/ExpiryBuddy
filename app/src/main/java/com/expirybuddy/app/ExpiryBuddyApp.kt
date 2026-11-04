package com.expirybuddy.app

import android.app.Application
import android.util.Log
import com.expirybuddy.app.reminders.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpiryBuddyApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)

        // Off the main thread: re-plan upcoming reminders in case the time zone changed.
        appScope.launch {
            try {
                container.reminderScheduler.restore(container.itemRepository.getAllItems())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not restore reminders", e)
            }
        }
    }

    private companion object {
        const val TAG = "ExpiryBuddyApp"
    }
}
