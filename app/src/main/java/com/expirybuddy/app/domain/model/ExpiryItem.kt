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
 * @property photoUri optional photo, a URI string for a file inside the app's private storage.
 * @property updatedAt moment the item was last saved.
 * @property usedAt moment the user marked the item as used, or null while it is still active. A used
 * item stays in the database (it is never silently deleted) but leaves the active lists and has no
 * reminder.
 */
data class ExpiryItem(
    val id: Long = NEW_ID,
    val name: String,
    val category: ItemCategory,
    val expiryDate: LocalDate,
    val notes: String? = null,
    val reminderDaysBefore: Int? = null,
    val createdAt: Instant,
    val photoUri: String? = null,
    val updatedAt: Instant = createdAt,
    val usedAt: Instant? = null,
) {
    val isUsed: Boolean get() = usedAt != null

    companion object {
        const val NEW_ID = 0L
    }
}
