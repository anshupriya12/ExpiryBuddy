package com.expirybuddy.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.model.ReminderOptions
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
                capitalization = KeyboardCapitalization.Sentences,
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

/** Reminder choice: no reminder, same day, or a number of days before. `null` means no reminder. */
@Composable
fun ReminderSelector(
    selectedDaysBefore: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_reminder),
) {
    MenuField(
        label = label,
        selected = selectedDaysBefore,
        options = ReminderOptions.ALL,
        optionText = { reminderLabel(it) },
        optionIcon = { Icons.Outlined.Notifications },
        onSelected = onSelected,
    )
}

/**
 * Read-only field that opens the platform Material date picker when tapped anywhere (not only on the icon).
 * The picker's dialog state is saved, so rotating the phone keeps it open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDateSelector(
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    label: String = stringResource(R.string.field_expiry_date),
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
            placeholder = {
                Text(
                    stringResource(R.string.field_expiry_date_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = stringResource(R.string.field_expiry_date_pick),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
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
