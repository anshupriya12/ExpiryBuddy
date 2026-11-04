package com.expirybuddy.app.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageState
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.reminderLabel
import java.time.ZoneId

@Composable
fun ItemDetailsScreen(
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ItemDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is DetailsUiState.Deleted) onNavigateBack()
    }

    val found = state as? DetailsUiState.Found

    ScreenScaffold(
        title = stringResource(R.string.details_title),
        onNavigateBack = onNavigateBack,
        actions = {
            if (found != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (val current = state) {
            DetailsUiState.Loading, DetailsUiState.Deleted -> LoadingState(contentModifier)
            DetailsUiState.NotFound -> MessageState(stringResource(R.string.item_not_found), contentModifier)
            DetailsUiState.Error -> MessageState(stringResource(R.string.details_load_failed), contentModifier)
            is DetailsUiState.Found -> ItemInfo(current, contentModifier)
        }
    }

    if (showDeleteDialog && found != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, found.item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDelete()
                    },
                ) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ItemInfo(found: DetailsUiState.Found, modifier: Modifier = Modifier) {
    val item = found.item
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = item.name, style = MaterialTheme.typography.headlineMedium)

        if (found.deleteFailed) {
            Text(
                text = stringResource(R.string.delete_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        InfoRow(stringResource(R.string.details_expiry_date), formatDate(item.expiryDate))
        InfoRow(stringResource(R.string.details_category), categoryLabel(item.category))
        InfoRow(stringResource(R.string.details_reminder), reminderLabel(item.reminderDaysBefore))
        InfoRow(
            stringResource(R.string.details_notes),
            item.notes ?: stringResource(R.string.details_no_notes),
        )
        InfoRow(stringResource(R.string.details_added), formatDate(item.addedDate()))
    }
}

/** createdAt is an instant; show it as a date in the user's current zone. */
private fun ExpiryItem.addedDate() = createdAt.atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
