package com.expirybuddy.app.domain.expiry

import java.time.LocalDate

/** Unit of the optional "use within" duration entered after opening a product. */
enum class UseWithinUnit(val storageKey: String) {
    DAYS("days"),
    MONTHS("months"),
    ;

    /** Largest amount accepted, so a typo cannot push a deadline centuries away. */
    val maxAmount: Int get() = if (this == DAYS) 3650 else 120

    companion object {
        fun fromStorageKey(key: String?): UseWithinUnit = entries.firstOrNull { it.storageKey == key } ?: DAYS
    }
}

/**
 * Pure rule for the use-by deadline when it is worked out from an opening date:
 * use-by date = opened on + use within. Months follow the calendar (31 Jan + 1 month = 28/29 Feb).
 */
object UseByCalculator {
    fun isValidAmount(amount: Int?, unit: UseWithinUnit): Boolean = amount != null && amount in 1..unit.maxAmount

    /** The calculated use-by date, or null when the opening date or a valid duration is missing. */
    fun calculate(openedOn: LocalDate?, amount: Int?, unit: UseWithinUnit): LocalDate? {
        if (openedOn == null || amount == null || !isValidAmount(amount, unit)) return null
        return when (unit) {
            UseWithinUnit.DAYS -> openedOn.plusDays(amount.toLong())
            UseWithinUnit.MONTHS -> openedOn.plusMonths(amount.toLong())
        }
    }
}
