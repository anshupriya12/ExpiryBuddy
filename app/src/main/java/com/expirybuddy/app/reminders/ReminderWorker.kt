package com.expirybuddy.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.domain.expiry.ExpiryClassifier
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

/**
 * Runs when a reminder is due. It re-reads the item first, so a reminder for an item that was
 * deleted or edited since scheduling never produces a stale notification.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, ExpiryItem.NEW_ID)
        if (itemId == ExpiryItem.NEW_ID) return Result.failure()

        val container = (applicationContext as ExpiryBuddyApp).container
        return try {
            val item = container.itemRepository.getItem(itemId)
            val today = LocalDate.now(container.clock)
            if (item != null && ReminderCalculator.isRelevant(item, today)) {
                NotificationHelper.showReminder(
                    applicationContext,
                    item,
                    ExpiryClassifier.daysUntil(item.expiryDate, today),
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Most likely a transient database problem: try again a couple of times, then give up.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        private const val MAX_ATTEMPTS = 3
    }
}
