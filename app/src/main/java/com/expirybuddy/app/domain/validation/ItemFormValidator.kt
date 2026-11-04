package com.expirybuddy.app.domain.validation

import java.time.LocalDate

enum class NameError { BLANK }

enum class DateError { MISSING }

data class FormValidation(
    val nameError: NameError? = null,
    val dateError: DateError? = null,
) {
    val isValid: Boolean get() = nameError == null && dateError == null
}

/**
 * Pure form rules, kept out of the UI so they are unit-testable. Errors are enums; the UI maps
 * them to localized strings. Length limits are enforced as input limits (see the constants), so
 * there is no "too long" error to show.
 */
object ItemFormValidator {
    const val MAX_NAME_LENGTH = 80
    const val MAX_NOTES_LENGTH = 500

    fun validate(name: String, expiryDate: LocalDate?): FormValidation = FormValidation(
        nameError = if (name.isBlank()) NameError.BLANK else null,
        dateError = if (expiryDate == null) DateError.MISSING else null,
    )
}
