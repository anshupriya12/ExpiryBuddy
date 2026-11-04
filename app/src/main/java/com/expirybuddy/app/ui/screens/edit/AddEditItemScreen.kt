package com.expirybuddy.app.ui.screens.edit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.model.ReminderOptions
import com.expirybuddy.app.domain.validation.DateError
import com.expirybuddy.app.domain.validation.NameError
import com.expirybuddy.app.reminders.needsNotificationPermission
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageState
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.util.DatePickerMillis
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.reminderLabel
import java.time.LocalDate

@Composable
fun AddEditItemScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPrefs = remember { (context.applicationContext as ExpiryBuddyApp).container.notificationPrefs }

    // The result doesn't matter: with or without the permission, the item is saved next.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onNotificationExplanationHandled()
    }
    val onSaveClick = {
        // Explain once, at the moment a reminder is actually being saved on Android 13+.
        viewModel.onSave(
            canExplainNotifications = context.needsNotificationPermission() && !notificationPrefs.hasExplained(),
        )
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
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

    val showForm = !state.isLoading && state.loadFailure == null
    ScreenScaffold(
        title = stringResource(if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_add),
        // The whole screen sits above the keyboard, so the pinned Save bar stays visible while typing.
        modifier = Modifier.imePadding(),
        onNavigateBack = onNavigateBack,
        bottomBar = { if (showForm) SaveBar(isSaving = state.isSaving, onClick = onSaveClick) },
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
            else -> ItemForm(state, viewModel, onSaveClick, contentModifier)
        }
    }
}

@Composable
private fun ItemForm(
    state: AddEditUiState,
    viewModel: AddEditViewModel,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_name)) },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = state.nameError?.let { error ->
                { Text(stringResource(nameErrorText(error))) }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        )

        DropdownField(
            label = stringResource(R.string.field_category),
            selectedText = categoryLabel(state.category),
            options = ItemCategory.entries,
            optionLabel = { categoryLabel(it) },
            onSelected = viewModel::onCategoryChange,
        )

        ExpiryDateField(
            date = state.expiryDate,
            error = state.dateError,
            onClick = { showDatePicker = true },
        )

        DropdownField(
            label = stringResource(R.string.field_reminder),
            selectedText = reminderLabel(state.reminderDaysBefore),
            options = ReminderOptions.ALL,
            optionLabel = { reminderLabel(it) },
            onSelected = viewModel::onReminderChange,
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp),
            label = { Text(stringResource(R.string.field_notes)) },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.edit_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

    }

    if (showDatePicker) {
        ExpiryDatePickerDialog(
            initialDate = state.expiryDate,
            onConfirm = {
                viewModel.onExpiryDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** Pinned below the scrolling form so Save never hides behind the keyboard. */
@Composable
private fun SaveBar(isSaving: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onClick,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun NotificationExplanationDialog(onContinue: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.notif_explain_title)) },
        text = { Text(stringResource(R.string.notif_explain_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.notif_explain_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.notif_explain_skip)) }
        },
    )
}

private fun nameErrorText(error: NameError): Int = when (error) {
    NameError.BLANK -> R.string.error_name_blank
}

private fun dateErrorText(error: DateError): Int = when (error) {
    DateError.MISSING -> R.string.error_date_missing
}

/** Read-only field that opens the date picker when tapped anywhere (not just the icon). */
@Composable
private fun ExpiryDateField(
    date: LocalDate?,
    error: DateError?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) onClick()
        }
    }

    OutlinedTextField(
        value = date?.let { formatDate(it) }.orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(stringResource(R.string.field_expiry_date)) },
        placeholder = { Text(stringResource(R.string.field_expiry_date_hint)) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(R.string.field_expiry_date_pick),
            )
        },
        isError = error != null,
        supportingText = error?.let { e -> { Text(stringResource(dateErrorText(e))) } },
        singleLine = true,
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryDatePickerDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.let(DatePickerMillis::fromLocalDate),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onConfirm(DatePickerMillis.toLocalDate(it)) }
                        ?: onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
