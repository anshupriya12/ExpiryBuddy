package com.expirybuddy.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import com.expirybuddy.app.domain.reminder.ReminderCalculator
import com.expirybuddy.app.domain.model.ItemCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware date, e.g. "12 Mar 2026" or "Mar 12, 2026". */
@Composable
fun formatDate(date: LocalDate): String {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    }
    return date.format(formatter)
}

@Composable
fun categoryLabel(category: ItemCategory): String = stringResource(
    when (category) {
        ItemCategory.FOOD -> R.string.category_food
        ItemCategory.MEDICATION -> R.string.category_medication
        ItemCategory.COSMETICS -> R.string.category_cosmetics
        ItemCategory.DOCUMENT -> R.string.category_document
        ItemCategory.HOUSEHOLD -> R.string.category_household
        ItemCategory.OTHER -> R.string.category_other
    },
)

@Composable
fun statusLabel(status: ExpiryStatus): String = stringResource(
    when (status) {
        ExpiryStatus.EXPIRED -> R.string.status_expired
        ExpiryStatus.EXPIRING_SOON -> R.string.status_expiring_soon
        ExpiryStatus.UPCOMING -> R.string.status_upcoming
    },
)

/** "Use by today", "Use within 5 days", "Past use-by date by 3 days"... for a day count from [ExpiryClassifier.daysUntil]. */
@Composable
fun expiryCountdownText(daysUntilExpiry: Long): String {
    val days = daysUntilExpiry.toInt()
    return when {
        days < 0 -> pluralStringResource(R.plurals.status_expired_days_ago, -days, -days)
        days == 0 -> stringResource(R.string.status_use_today)
        days == 1 -> stringResource(R.string.status_use_tomorrow)
        else -> pluralStringResource(R.plurals.status_use_in_days, days, days)
    }
}

@Composable
fun reminderLabel(daysBefore: Int?): String = when (daysBefore) {
    null -> stringResource(R.string.reminder_none)
    0 -> stringResource(R.string.reminder_on_day)
    else -> pluralStringResource(R.plurals.reminder_days_before, daysBefore, daysBefore)
}

/** A real moment (like "used on") shown as a date in the user's current time zone. */
@Composable
fun formatInstantDate(instant: Instant): String =
    formatDate(instant.atZone(ZoneId.systemDefault()).toLocalDate())

/** Locale-aware time of day such as "7:30 PM" or "19:30" (follows the phone's 12/24-hour setting). */
@Composable
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val time = ReminderCalculator.timeOfMinutes(minuteOfDay)
    return remember(minuteOfDay, is24) {
        val pattern = if (is24) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        pattern.withLocale(Locale.getDefault()).format(time)
    }
}

/** "2 days after opening" / "1 month after opening". */
@Composable
fun useWithinLabel(amount: Int, unit: UseWithinUnit): String = pluralStringResource(
    if (unit == UseWithinUnit.DAYS) R.plurals.details_use_within_days else R.plurals.details_use_within_months,
    amount,
    amount,
)
