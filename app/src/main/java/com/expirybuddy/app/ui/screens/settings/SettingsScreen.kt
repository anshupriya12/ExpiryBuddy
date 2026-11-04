package com.expirybuddy.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.expirybuddy.app.BuildConfig
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import com.expirybuddy.app.reminders.canPostNotifications
import com.expirybuddy.app.reminders.needsNotificationPermission
import com.expirybuddy.app.ui.components.ScreenScaffold
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(context.canPostNotifications()) }
    // Set once the system permission prompt has been declined; it may not show again after that.
    var requestDeclined by rememberSaveable { mutableStateOf(false) }

    // The user can change this in system settings, so re-check whenever the screen is shown again.
    LifecycleResumeEffect(Unit) {
        notificationsOn = context.canPostNotifications()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = context.canPostNotifications()
        if (!granted) requestDeclined = true
    }

    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        onNavigateBack = onNavigateBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NotificationsCard(
                enabled = notificationsOn,
                canAskPermission = context.needsNotificationPermission() && !requestDeclined,
                onAllow = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                onOpenSystemSettings = { openNotificationSettings(context) },
            )
            AboutCard()
        }
    }
}

@Composable
private fun NotificationsCard(
    enabled: Boolean,
    canAskPermission: Boolean,
    onAllow: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.settings_notifications_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    if (enabled) R.string.settings_notifications_on else R.string.settings_notifications_off,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!enabled) {
                Button(
                    onClick = if (canAskPermission) onAllow else onOpenSystemSettings,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(
                            if (canAskPermission) R.string.settings_allow_notifications else R.string.settings_open_system_settings,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val reminderTime = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(ReminderCalculator.REMINDER_TIME)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.settings_reminder_time, reminderTime),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
