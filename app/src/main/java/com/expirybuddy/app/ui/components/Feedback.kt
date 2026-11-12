package com.expirybuddy.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors
import kotlinx.coroutines.launch

/**
 * Shows a confirmation left by the previous screen (see `Destinations.MESSAGE_KEY`) as a snackbar, once.
 * [onShown] clears the stored message straight away so rotating or returning later does not replay it.
 */
@Composable
fun MessageEffect(@StringRes message: Int?, hostState: SnackbarHostState, onShown: () -> Unit) {
    val text = message?.let { stringResource(it) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (text != null) {
            onShown()
            // Launched separately: clearing the message changes this effect's key and would cancel it.
            scope.launch { hostState.showSnackbar(text) }
        }
    }
}

/** Hosts the short confirmation messages shown after save, delete and mark-as-used. */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> AppSnackbar(data) }
}

/** White card with a green check, the message and a close button. Announced by screen readers when it appears. */
@Composable
fun AppSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    Snackbar(
        modifier = modifier
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // The default dismiss colour is meant for a dark snackbar and vanishes on this white card.
        dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        dismissAction = {
            IconButton(onClick = { data.dismiss() }) {
                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.action_dismiss))
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.expiryColors.success),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                )
            }
            Text(data.visuals.message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Asks before an item is permanently removed. Cancel is the safe, outlined choice. */
@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message, itemName)) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.delete_confirm),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
            )
        },
    )
}
