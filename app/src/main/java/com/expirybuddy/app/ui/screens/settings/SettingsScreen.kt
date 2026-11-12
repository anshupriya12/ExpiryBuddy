package com.expirybuddy.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.BuildConfig
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ReminderOptions
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import com.expirybuddy.app.reminders.canPostNotifications
import com.expirybuddy.app.reminders.needsNotificationPermission
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.AppIcons
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.components.SecondaryButton
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.util.reminderLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SettingsScreen(
    onBackToHome: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val defaultReminder by viewModel.defaultReminderDays.collectAsStateWithLifecycle()
    var notificationsOn by remember { mutableStateOf(context.canPostNotifications()) }
    var permissionMissing by remember { mutableStateOf(context.needsNotificationPermission()) }
    // Set once the system permission prompt has been declined; it may not show again after that.
    var requestDeclined by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    // The user can change this in system settings, so re-check whenever the screen is shown again.
    LifecycleResumeEffect(Unit) {
        notificationsOn = context.canPostNotifications()
        permissionMissing = context.needsNotificationPermission()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = context.canPostNotifications()
        permissionMissing = context.needsNotificationPermission()
        if (!granted) requestDeclined = true
    }

    val reminderTime = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(ReminderCalculator.REMINDER_TIME)
    }

    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        onNavigateBack = onBackToHome,
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.page, vertical = Spacing.md),
            ) {
                SecondaryButton(
                    text = stringResource(R.string.settings_back_home),
                    onClick = onBackToHome,
                    leadingIcon = Icons.Outlined.Home,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SettingsCard {
                // Never shows "on" unless the system really allows notifications.
                NotificationRow(
                    enabled = notificationsOn,
                    subtitle = stringResource(
                        when {
                            notificationsOn -> R.string.settings_notifications_on
                            permissionMissing && !requestDeclined -> R.string.settings_notifications_off
                            else -> R.string.settings_notifications_blocked
                        },
                    ),
                    onToggle = {
                        if (permissionMissing && !requestDeclined) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // The app cannot switch notifications on or off itself once the system owns the choice.
                            openNotificationSettings(context)
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DefaultReminderRow(selected = defaultReminder, onSelected = viewModel::onDefaultReminderChange)
            }
            Text(
                text = stringResource(R.string.settings_reminder_time, reminderTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )

            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .semantics { heading() },
            )
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = AppIcons.Phone,
                    title = stringResource(R.string.settings_privacy_title),
                    subtitle = stringResource(R.string.settings_privacy_summary),
                    onClick = { showPrivacy = true },
                    trailing = { Chevron() },
                )
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.settings_privacy_title)) },
            text = { Text(stringResource(R.string.settings_privacy)) },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.action_ok)) }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationRow(enabled: Boolean, subtitle: String, onToggle: () -> Unit) {
    // One toggleable row: the switch itself is not a separate control for screen readers.
    SettingsRow(
        icon = Icons.Outlined.Notifications,
        title = stringResource(R.string.settings_notifications_title),
        subtitle = subtitle,
        modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() }),
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    )
}

@Composable
private fun DefaultReminderRow(selected: Int?, onSelected: (Int?) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        SettingsRow(
            icon = Icons.Outlined.DateRange,
            title = stringResource(R.string.settings_default_reminder),
            subtitle = reminderLabel(selected),
            onClick = { expanded = true },
            trailing = { Chevron() },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            ReminderOptions.ALL.forEach { option ->
                DropdownMenuItem(
                    text = { Text(reminderLabel(option)) },
                    trailingIcon = {
                        if (option == selected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    modifier = Modifier.heightIn(min = Spacing.touchTarget),
                )
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
