package com.expirybuddy.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.expirybuddy.app.MainActivity
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ExpiryItem

object NotificationHelper {
    const val CHANNEL_ID = "expiry_reminders"

    /** Safe to call repeatedly; the system ignores an existing channel. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Shows the reminder for [item]. Returns false (and shows nothing) when notifications are not
     * allowed, which is a normal situation, not an error.
     *
     * @param daysUntilExpiry whole days from today to the expiry date (0 = today).
     */
    fun showReminder(context: Context, item: ExpiryItem, daysUntilExpiry: Long): Boolean {
        if (!context.canPostNotifications()) return false

        val text = when (daysUntilExpiry) {
            0L -> context.getString(R.string.status_expires_today)
            1L -> context.getString(R.string.status_expires_tomorrow)
            else -> context.resources.getQuantityString(
                R.plurals.status_expires_in_days,
                daysUntilExpiry.toInt(),
                daysUntilExpiry.toInt(),
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.name)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openItemIntent(context, item.id))
            .build()

        // One notification per item: a repeat for the same item updates it instead of stacking.
        context.getSystemService(NotificationManager::class.java).notify(notificationId(item.id), notification)
        return true
    }

    fun notificationId(itemId: Long): Int = itemId.toInt()

    private fun openItemIntent(context: Context, itemId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ITEM_ID, itemId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
