package com.expirybuddy.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import com.expirybuddy.app.domain.model.ReminderOptions
import com.expirybuddy.app.domain.model.StorageType
import com.expirybuddy.app.ui.util.storageTypeLabel
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.util.DatePickerMillis
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.reminderLabel
import java.time.LocalDate

/**
 * A visible label above its field, the layout used by every form field in the app. The label and the
 * field are merged into one accessibility node, so a screen reader announces "Name, edit box" as one
 * control instead of an unlabelled box next to a loose piece of text.
 */
@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

/** Text field with a label above it, a placeholder and an inline error. */
@Composable
fun AddItemFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    LabeledField(label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            singleLine = singleLine,
            minLines = minLines,
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            shape = MaterialTheme.shapes.small,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
        )
    }
}

/** Read-only field that opens a menu. Each option can have an icon; the current one gets a check mark. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MenuField(
    label: String,
    selected: T,
    options: List<T>,
    optionText: @Composable (T) -> String,
    optionIcon: (T) -> ImageVector,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    LabeledField(label) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = optionText(selected),
                onValueChange = {},
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                readOnly = true,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        optionIcon(selected),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option), style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = {
                            Icon(
                                optionIcon(option),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        },
                        trailingIcon = {
                            if (option == selected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        modifier = Modifier.heightIn(min = Spacing.touchTarget),
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    selected: ItemCategory,
    onSelected: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_category),
) {
    MenuField(
        label = label,
        selected = selected,
        options = ItemCategory.entries,
        optionText = { categoryLabel(it) },
        optionIcon = ::categoryIcon,
        onSelected = onSelected,
    )
}

/** Sentinel for the "Custom" entry in the advance-notice menu (never a real number of days). */
private const val CUSTOM_CHOICE = -2

/**
 * "How early" choice: no reminder, the use-by date itself, 1 to 3 days before, or a custom number of
 * days. `null` means no reminder. When [isCustom] is true the number of days is typed by the user
 * (see [CustomDaysField]) and the menu shows "Custom".
 */
@Composable
fun AdvanceNoticeSelector(
    selectedDaysBefore: Int?,
    isCustom: Boolean,
    onPresetSelected: (Int?) -> Unit,
    onCustomSelected: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_reminder),
) {
    val options: List<Int?> = ReminderOptions.PRESETS + CUSTOM_CHOICE
    MenuField(
        label = label,
        selected = if (isCustom) CUSTOM_CHOICE else selectedDaysBefore,
        options = options,
        optionText = { if (it == CUSTOM_CHOICE) stringResource(R.string.reminder_custom) else reminderLabel(it) },
        optionIcon = { Icons.Outlined.Notifications },
        onSelected = { if (it == CUSTOM_CHOICE) onCustomSelected() else onPresetSelected(it) },
    )
}

/** Number-of-days field shown under the advance-notice menu when "Custom" is chosen. */
@Composable
fun CustomDaysField(
    text: String,
    onTextChange: (String) -> Unit,
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    AddItemFormField(
        label = stringResource(R.string.reminder_custom_days_label),
        value = text,
        onValueChange = onTextChange,
        modifier = modifier,
        placeholder = stringResource(R.string.reminder_custom_days_hint),
        errorText = errorText,
        keyboardType = KeyboardType.Number,
    )
}

/** Number plus unit for "use within": e.g. 2 + Days. */
@Composable
fun UseWithinField(
    amountText: String,
    unit: UseWithinUnit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (UseWithinUnit) -> Unit,
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) {
            AddItemFormField(
                label = stringResource(R.string.field_use_within),
                value = amountText,
                onValueChange = onAmountChange,
                placeholder = stringResource(R.string.field_use_within_hint),
                errorText = errorText,
                keyboardType = KeyboardType.Number,
            )
        }
        Box(Modifier.weight(1f)) {
            MenuField(
                label = stringResource(R.string.field_unit),
                selected = unit,
                options = UseWithinUnit.entries,
                optionText = { stringResource(if (it == UseWithinUnit.DAYS) R.string.unit_days else R.string.unit_months) },
                optionIcon = { Icons.Outlined.DateRange },
                onSelected = onUnitChange,
            )
        }
    }
}

/**
 * Read-only field that opens the platform Material date picker when tapped anywhere (not only on the icon).
 * The picker's dialog state is saved, so rotating the phone keeps it open. An optional [onClear] shows a
 * clear button, used by fields that may stay empty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    label: String,
    hint: String,
    pickDescription: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    supportingText: String? = null,
    onClear: (() -> Unit)? = null,
    clearDescription: String = "",
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val openPicker by rememberUpdatedState { showPicker = true }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) openPicker()
        }
    }

    LabeledField(label) {
        OutlinedTextField(
            value = date?.let { formatDate(it) }.orEmpty(),
            onValueChange = {},
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            readOnly = true,
            singleLine = true,
            placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = pickDescription,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            trailingIcon = if (onClear != null && date != null) {
                {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, contentDescription = clearDescription)
                    }
                }
            } else {
                null
            },
            isError = errorText != null,
            supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
            shape = MaterialTheme.shapes.small,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = fieldColors(),
            interactionSource = interactionSource,
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.let(DatePickerMillis::fromLocalDate),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPicker = false
                        pickerState.selectedDateMillis?.let { onDateSelected(DatePickerMillis.toLocalDate(it)) }
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * Read-only field showing a time of day; tapping it opens the Material time picker. Used for the
 * per-item reminder time, where "no time chosen" means "follow the default" ([valueText] says so).
 * The dialog state is saved, so rotating the phone keeps it open.
 */
@Composable
fun TimeSelector(
    label: String,
    valueText: String,
    initialMinuteOfDay: Int,
    dialogTitle: String,
    onTimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val openPicker by rememberUpdatedState { showPicker = true }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) openPicker()
        }
    }

    LabeledField(label) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            readOnly = true,
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            },
            supportingText = supportingText?.let { { Text(it) } },
            shape = MaterialTheme.shapes.small,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = fieldColors(),
            interactionSource = interactionSource,
        )
    }

    if (showPicker) {
        ReminderTimePickerDialog(
            title = dialogTitle,
            initialMinuteOfDay = initialMinuteOfDay,
            onConfirm = {
                showPicker = false
                onTimeSelected(it)
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** The platform Material time picker in a dialog; follows the phone's 12 or 24-hour setting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    title: String,
    initialMinuteOfDay: Int,
    onConfirm: (minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = is24Hour,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Optional storage type: "Not specified" or Refrigerate, Frozen, Airtight. `null` means not specified. */
@Composable
fun StorageTypeSelector(
    selected: StorageType?,
    onSelected: (StorageType?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_storage_type),
) {
    val options: List<StorageType?> = listOf<StorageType?>(null) + StorageType.entries
    MenuField(
        label = label,
        selected = selected,
        options = options,
        optionText = { storageTypeLabel(it) },
        optionIcon = { AppIcons.Box },
        onSelected = onSelected,
    )
}
