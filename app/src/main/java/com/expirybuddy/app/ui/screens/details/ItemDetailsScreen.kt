package com.expirybuddy.app.ui.screens.details

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.AppIcons
import com.expirybuddy.app.ui.components.DeleteConfirmationDialog
import com.expirybuddy.app.ui.components.ExpiryStatusLabel
import com.expirybuddy.app.ui.components.ItemPhoto
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageEffect
import com.expirybuddy.app.ui.components.MessageState
import com.expirybuddy.app.ui.components.PrimaryButton
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.components.SecondaryButton
import com.expirybuddy.app.ui.components.categoryIcon
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.formatInstantDate
import com.expirybuddy.app.ui.util.reminderLabel
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun ItemDetailsScreen(
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onMarkedUsed: () -> Unit,
    snackbarHostState: SnackbarHostState,
    @StringRes message: Int?,
    onMessageShown: () -> Unit,
    viewModel: ItemDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val restoredText = stringResource(R.string.message_restored)

    MessageEffect(message, snackbarHostState, onMessageShown)
    LaunchedEffect(state) {
        when (state) {
            DetailsUiState.Deleted -> onDeleted()
            DetailsUiState.MarkedUsed -> onMarkedUsed()
            else -> Unit
        }
    }

    val found = state as? DetailsUiState.Found

    ScreenScaffold(
        title = stringResource(R.string.details_title),
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (found != null) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.height(Spacing.touchTarget),
                    )
                }
            }
        },
        bottomBar = {
            if (found != null) {
                ActionBar(
                    isUsed = found.item.isUsed,
                    enabled = true,
                    onToggleUsed = {
                        if (found.item.isUsed) {
                            viewModel.onRestore()
                            scope.launch { snackbarHostState.showSnackbar(restoredText) }
                        } else {
                            viewModel.onMarkUsed()
                        }
                    },
                    onEdit = onEdit,
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (val current = state) {
            DetailsUiState.Loading, DetailsUiState.Deleted, DetailsUiState.MarkedUsed -> LoadingState(contentModifier)
            DetailsUiState.NotFound -> MessageState(stringResource(R.string.item_not_found), contentModifier)
            DetailsUiState.Error -> MessageState(stringResource(R.string.details_load_failed), contentModifier)
            is DetailsUiState.Found -> ItemInfo(current, contentModifier)
        }
    }

    if (showDeleteDialog && found != null) {
        DeleteConfirmationDialog(
            itemName = found.item.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun ActionBar(
    isUsed: Boolean,
    enabled: Boolean,
    onToggleUsed: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.page, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PrimaryButton(
                text = stringResource(if (isUsed) R.string.details_restore else R.string.details_mark_used),
                onClick = onToggleUsed,
                enabled = enabled,
                leadingIcon = Icons.Outlined.Check,
                // The longer label gets more room, so it stays on one line at normal font size.
                modifier = Modifier.weight(1.35f),
            )
            SecondaryButton(
                text = stringResource(R.string.details_edit),
                onClick = onEdit,
                leadingIcon = Icons.Outlined.Edit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ItemInfo(found: DetailsUiState.Found, modifier: Modifier = Modifier) {
    val item = found.item
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Hero(item)

        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        CategoryPill(item)
        StatusPanel(found)

        if (found.deleteFailed) ErrorText(R.string.delete_failed)
        if (found.updateFailed) ErrorText(R.string.details_update_failed)

        Column {
            InfoRow(Icons.Outlined.DateRange, stringResource(R.string.details_expiry_date), formatDate(item.expiryDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(
                Icons.Outlined.Notifications,
                stringResource(R.string.details_reminder),
                if (item.isUsed) stringResource(R.string.reminder_none) else reminderLabel(item.reminderDaysBefore),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(
                AppIcons.Document,
                stringResource(R.string.details_notes),
                item.notes ?: stringResource(R.string.details_no_notes),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(AppIcons.Box, stringResource(R.string.details_added), formatDate(item.addedDate()))
        }
    }
}

@Composable
private fun Hero(item: ExpiryItem) {
    val shape = MaterialTheme.shapes.large
    if (item.photoUri != null) {
        ItemPhoto(
            photoUri = item.photoUri,
            contentDescription = stringResource(R.string.photo_preview_description),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shape)
                .background(MaterialTheme.expiryColors.peach),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(item.category),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CategoryPill(item: ExpiryItem) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.expiryColors.peach)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            categoryIcon(item.category),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            categoryLabel(item.category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** The status in words plus a colour cue, e.g. "Expires in 2 days", or "Used on ..." once marked used. */
@Composable
private fun StatusPanel(found: DetailsUiState.Found) {
    val item = found.item
    val used = item.usedAt
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (used != null) MaterialTheme.expiryColors.successContainer else MaterialTheme.expiryColors.peach,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = if (used != null) Icons.Outlined.Check else Icons.Outlined.DateRange,
                contentDescription = null,
                tint = if (used != null) MaterialTheme.expiryColors.success else MaterialTheme.colorScheme.secondary,
            )
            Column {
                if (used != null) {
                    Text(
                        text = stringResource(R.string.status_used_on, formatInstantDate(used)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.expiryColors.success,
                    )
                } else {
                    ExpiryStatusLabel(
                        status = found.status,
                        daysUntilExpiry = found.daysUntilExpiry,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = formatDate(item.expiryDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorText(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** createdAt is an instant; show it as a date in the user's current zone. */
private fun ExpiryItem.addedDate() = createdAt.atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
