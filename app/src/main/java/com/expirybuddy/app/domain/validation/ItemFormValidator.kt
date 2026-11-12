package com.expirybuddy.app.domain.validation

import com.expirybuddy.app.domain.expiry.UseByCalculator
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import com.expirybuddy.app.domain.model.ReminderOptions
import java.time.LocalDate

enum class NameError { BLANK }

/** The use-by date is the one required deadline. */
enum class DateError { MISSING }

/** The optional "use within" number is not a whole number in the allowed range. */
enum class UseWithinError { INVALID }

/** A custom "days before" reminder is not a whole number in the allowed range. */
enum class CustomReminderError { INVALID }

data class FormValidation(
    val nameError: NameError? = null,
    val dateError: DateError? = null,
    val useWithinError: UseWithinError? = null,
    val customReminderError: CustomReminderError? = null,
) {
    val isValid: Boolean
        get() = nameError == null && dateError == null && useWithinError == null && customReminderError == null
}

/**
 * Pure form rules, kept out of the UI so they are unit-testable. Errors are enums; the UI maps
 * them to localized strings. Length limits are enforced as input limits (see the constants), so
 * there is no "too long" error to show.
 */
object ItemFormValidator {
    const val MAX_NAME_LENGTH = 80
    const val MAX_NOTES_LENGTH = 500
    const val MAX_STORAGE_LENGTH = 500

    fun validate(
        name: String,
        useByDate: LocalDate?,
        useWithinText: String = "",
        useWithinUnit: UseWithinUnit = UseWithinUnit.DAYS,
        customReminderText: String? = null,
    ): FormValidation = FormValidation(
        nameError = if (name.isBlank()) NameError.BLANK else null,
        dateError = if (useByDate == null) DateError.MISSING else null,
        useWithinError = if (useWithinText.isNotBlank() &&
            !UseByCalculator.isValidAmount(useWithinText.trim().toIntOrNull(), useWithinUnit)
        ) {
            UseWithinError.INVALID
        } else {
            null
        },
        customReminderError = if (customReminderText != null &&
            !ReminderOptions.isValidCustom(customReminderText.trim().toIntOrNull())
        ) {
            CustomReminderError.INVALID
        } else {
            null
        },
    )
}
