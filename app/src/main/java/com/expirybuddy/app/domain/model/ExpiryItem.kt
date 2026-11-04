package com.expirybuddy.app.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * An item the user wants to be reminded about.
 *
 * @property id database id; [NEW_ID] (0) for an item that has not been saved yet.
 * @property expiryDate a calendar date with no time zone: "expires on 12 March" means the same
 * day wherever the user is.
 * @property reminderDaysBefore days before [expiryDate] to remind (0 = on the day itself);
 * null means the user chose no reminder.
 * @property createdAt moment the item was first saved. A real instant, unlike [expiryDate].
 */
data class ExpiryItem(
    val id: Long = NEW_ID,
    val name: String,
    val category: ItemCategory,
    val expiryDate: LocalDate,
    val notes: String? = null,
    val reminderDaysBefore: Int? = null,
    val createdAt: Instant,
) {
    companion object {
        const val NEW_ID = 0L
    }
}
