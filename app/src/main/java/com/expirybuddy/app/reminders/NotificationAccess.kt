package com.expirybuddy.app.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** True on Android 13+ when the notification permission has not been granted. */
fun Context.needsNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** True when a notification would actually be shown (permission granted and not switched off in system settings). */
fun Context.canPostNotifications(): Boolean =
    !needsNotificationPermission() && NotificationManagerCompat.from(this).areNotificationsEnabled()
