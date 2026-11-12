package com.expirybuddy.app.ui.screens.edit

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.validation.DateError
import com.expirybuddy.app.domain.validation.NameError
import com.expirybuddy.app.reminders.needsNotificationPermission
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.AddItemFormField
import com.expirybuddy.app.ui.components.CategorySelector
import com.expirybuddy.app.ui.components.DeleteConfirmationDialog
import com.expirybuddy.app.ui.components.ExpiryDateSelector
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageState
import com.expirybuddy.app.ui.components.NeutralIconButton
import com.expirybuddy.app.ui.components.PhotoPicker
import com.expirybuddy.app.ui.components.PrimaryButton
import com.expirybuddy.app.ui.components.ReminderSelector
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.components.SecondaryButton
import com.expirybuddy.app.ui.theme.Spacing

/**
 * The one form for adding and editing an item. Values live in the ViewModel, so rotating the phone or
 * failing validation never clears what was typed.
 *
 * @param onSaved called once the item is saved; the argument is true when an existing item was edited.
 * @param onDeleted called once an item was deleted from this screen.
 */
@Composable
fun AddEditItemScreen(
    onNavigateBack: () -> Unit,
    onSaved: (wasEditing: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: AddEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ExpiryBuddyApp).container }
    val notificationPrefs = container.notificationPrefs
    val photoStorage = container.photoStorage
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // The result doesn't matter: with or without the permission, the item is saved next.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onNotificationExplanationHandled()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }
    // Saved, so the target survives the camera app forcing this process to restart on a low-memory phone.
    var pendingCameraTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val target = pendingCameraTarget
        pendingCameraTarget = null
        if (taken && target != null) viewModel.onPhotoPicked(target)
    }

    val onSaveClick = {
        // Explain once, at the moment a reminder is actually being saved on Android 13+.
        viewModel.onSave(
            canExplainNotifications = context.needsNotificationPermission() && !notificationPrefs.hasExplained(),
        )
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved(state.isEditing)
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    if (state.showNotificationExplanation) {
        NotificationExplanationDialog(
            onContinue = {
                notificationPrefs.markExplained()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onSkip = {
                notificationPrefs.markExplained()
                viewModel.onNotificationExplanationHandled()
            },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemName = state.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val showForm = !state.isLoading && state.loadFailure == null
    ScreenScaffold(
        title = stringResource(if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_add),
        // The whole screen sits above the keyboard, so the pinned Save bar stays visible while typing.
        modifier = Modifier.imePadding(),
        onNavigateBack = onNavigateBack,
        bottomBar = {
            if (showForm) {
                SaveBar(
                    isEditing = state.isEditing,
                    busy = state.isSaving || state.isDeleting || state.isPhotoBusy,
                    onSave = onSaveClick,
                    onDelete = { showDeleteDialog = true },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.isLoading -> LoadingState(contentModifier)
            state.loadFailure != null -> MessageState(
                body = stringResource(
                    if (state.loadFailure == LoadFailure.NOT_FOUND) R.string.item_not_found else R.string.edit_load_failed,
                ),
                modifier = contentModifier,
            )
            else -> ItemForm(
                state = state,
                viewModel = viewModel,
                canTakePhoto = remember { photoStorage.hasCamera() },
                onTakePhoto = {
                    photoStorage.createCameraTarget()?.let { target ->
                        pendingCameraTarget = target
                        cameraLauncher.launch(Uri.parse(target))
                    }
                },
                onChooseFromGallery = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun ItemForm(
    state: AddEditUiState,
    viewModel: AddEditViewModel,
    canTakePhoto: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        PhotoPicker(
            photoUri = state.photoUri,
            isBusy = state.isPhotoBusy,
            failed = state.photoFailed,
            canTakePhoto = canTakePhoto,
            onTakePhoto = onTakePhoto,
            onChooseFromGallery = onChooseFromGallery,
            onRemove = viewModel::onPhotoRemoved,
        )

        AddItemFormField(
            label = stringResource(R.string.field_name),
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = stringResource(R.string.field_name_hint),
            errorText = state.nameError?.let { stringResource(nameErrorText(it)) },
        )

        CategorySelector(selected = state.category, onSelected = viewModel::onCategoryChange)

        ExpiryDateSelector(
            date = state.expiryDate,
            onDateSelected = viewModel::onExpiryDateChange,
            errorText = state.dateError?.let { stringResource(dateErrorText(it)) },
        )

        ReminderSelector(
            selectedDaysBefore = state.reminderDaysBefore,
            onSelected = viewModel::onReminderChange,
        )

        AddItemFormField(
            label = stringResource(R.string.field_notes),
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            placeholder = stringResource(R.string.field_notes_hint),
            singleLine = false,
            minLines = 3,
            imeAction = ImeAction.Default,
        )

        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.edit_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.deleteFailed) {
            Text(
                text = stringResource(R.string.delete_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Pinned below the scrolling form so Save never hides behind the keyboard. Delete is separate and neutral. */
@Composable
private fun SaveBar(
    isEditing: Boolean,
    busy: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.page, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PrimaryButton(
                text = stringResource(if (isEditing) R.string.action_save_changes else R.string.action_save_item),
                onClick = onSave,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
            if (isEditing) {
                NeutralIconButton(
                    icon = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun NotificationExplanationDialog(onContinue: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        icon = {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text(stringResource(R.string.notif_explain_title)) },
        text = { Text(stringResource(R.string.notif_explain_body)) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.notif_explain_continue), onClick = onContinue)
        },
        dismissButton = {
            SecondaryButton(text = stringResource(R.string.notif_explain_skip), onClick = onSkip)
        },
    )
}

private fun nameErrorText(error: NameError): Int = when (error) {
    NameError.BLANK -> R.string.error_name_blank
}

private fun dateErrorText(error: DateError): Int = when (error) {
    DateError.MISSING -> R.string.error_date_missing
}
