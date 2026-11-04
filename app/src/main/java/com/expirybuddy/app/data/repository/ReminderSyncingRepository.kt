package com.expirybuddy.app.data.repository

import android.util.Log
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.reminders.ReminderScheduler

/**
 * Keeps reminders in step with the data: every save re-plans the item's reminder (replacing the old
 * one, or cancelling it if it no longer applies) and every delete cancels it. Doing this in one
 * place means no screen can forget it.
 *
 * Reminders are a convenience on top of the data, so a scheduling failure is logged but never makes
 * a successful save or delete look like it failed.
 */
class ReminderSyncingRepository(
    private val delegate: ExpiryItemRepository,
    private val scheduler: ReminderScheduler,
) : ExpiryItemRepository by delegate {

    override suspend fun save(item: ExpiryItem): Long {
        val id = delegate.save(item)
        syncReminder("schedule") { scheduler.schedule(item.copy(id = id)) }
        return id
    }

    override suspend fun delete(id: Long): Boolean {
        val deleted = delegate.delete(id)
        // Cancel even if nothing was deleted: harmless, and clears any orphaned reminder.
        syncReminder("cancel") { scheduler.cancel(id) }
        return deleted
    }

    private inline fun syncReminder(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Could not $action reminder", e)
        }
    }

    private companion object {
        const val TAG = "ReminderSync"
    }
}
