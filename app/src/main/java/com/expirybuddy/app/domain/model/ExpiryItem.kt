package com.expirybuddy.app.domain.model

import com.expirybuddy.app.domain.expiry.UseWithinUnit
import java.time.Instant
import java.time.LocalDate

/**
 * An item the user wants to be reminded about.
 *
 * @property id database id; [NEW_ID] (0) for an item that has not been saved yet.
 * @property expiryDate the item's **use-by date**, its one user-facing deadline. The name is kept
 * internally so no rename or data change is needed: dates saved before V2.2 are simply read as
 * use-by dates. A calendar date with no time zone: "use by 12 March" means the same day wherever
 * the user is.
 * @property openedOn optional date the product was opened. With [useWithinAmount] it is how the
 * use-by date can be worked out; the use-by date itself stays editable.
 * @property useWithinAmount optional duration after opening, in [useWithinUnit].
 * @property storageType optional quick choice: refrigerate, frozen or airtight.
 * @property storageInstructions optional free text ("additional instructions"), multiline. Text
 * saved before the storage type existed stays here unchanged.
 * @property reminderMinuteOfDay this item's reminder time in minutes after midnight (0..1439), or
 * null to follow the default notification time from Settings.
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
    val openedOn: LocalDate? = null,
    val useWithinAmount: Int? = null,
    val useWithinUnit: UseWithinUnit = UseWithinUnit.DAYS,
    val storageInstructions: String? = null,
    val storageType: StorageType? = null,
    val reminderMinuteOfDay: Int? = null,
) {
    val isUsed: Boolean get() = usedAt != null

    companion object {
        const val NEW_ID = 0L
    }
}
