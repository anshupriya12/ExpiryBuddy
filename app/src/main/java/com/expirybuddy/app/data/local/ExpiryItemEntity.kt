package com.expirybuddy.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expirybuddy.app.domain.expiry.UseWithinUnit
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.domain.model.StorageType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "items",
    indices = [Index(value = ["expiryDate"])],
)
data class ExpiryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [ItemCategory.storageKey]. */
    val category: String,
    /** Calendar date, stored as epoch day via [Converters]. */
    val expiryDate: LocalDate,
    val notes: String?,
    /** Null = no reminder. */
    val reminderDaysBefore: Int?,
    /** Epoch milliseconds. */
    val createdAt: Long,
    /** Added in schema version 2. URI string of a photo in app-private storage. */
    val photoUri: String? = null,
    /** Added in schema version 2. Epoch milliseconds; the migration copies [createdAt] into it. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    /** Added in schema version 2. Epoch milliseconds when marked as used; null while active. */
    val usedAt: Long? = null,
    /** Added in schema version 3. Optional date the product was opened (epoch day via [Converters]). */
    val openedOn: LocalDate? = null,
    /** Added in schema version 3. Optional "use within" amount, in [useWithinUnit]. */
    val useWithinAmount: Int? = null,
    /** Added in schema version 3. [UseWithinUnit.storageKey]; null when there is no duration. */
    val useWithinUnit: String? = null,
    /** Added in schema version 3. Optional storage instructions. */
    val storageInstructions: String? = null,
    /** Added in schema version 3. Per-item reminder time in minutes after midnight; null = follow the default. */
    val reminderMinuteOfDay: Int? = null,
    /** Added in schema version 4. [com.expirybuddy.app.domain.model.StorageType.storageKey]; null = not specified. */
    val storageType: String? = null,
)

fun ExpiryItemEntity.toDomain(): ExpiryItem = ExpiryItem(
    id = id,
    name = name,
    category = ItemCategory.fromStorageKey(category),
    expiryDate = expiryDate,
    notes = notes,
    reminderDaysBefore = reminderDaysBefore,
    createdAt = Instant.ofEpochMilli(createdAt),
    photoUri = photoUri,
    updatedAt = Instant.ofEpochMilli(updatedAt),
    usedAt = usedAt?.let(Instant::ofEpochMilli),
    openedOn = openedOn,
    useWithinAmount = useWithinAmount,
    useWithinUnit = UseWithinUnit.fromStorageKey(useWithinUnit),
    storageInstructions = storageInstructions,
    reminderMinuteOfDay = reminderMinuteOfDay,
    storageType = StorageType.fromStorageKey(storageType),
)

fun ExpiryItem.toEntity(): ExpiryItemEntity = ExpiryItemEntity(
    id = id,
    name = name,
    category = category.storageKey,
    expiryDate = expiryDate,
    notes = notes,
    reminderDaysBefore = reminderDaysBefore,
    createdAt = createdAt.toEpochMilli(),
    photoUri = photoUri,
    updatedAt = updatedAt.toEpochMilli(),
    usedAt = usedAt?.toEpochMilli(),
    openedOn = openedOn,
    useWithinAmount = useWithinAmount,
    useWithinUnit = useWithinUnit.storageKey.takeIf { useWithinAmount != null },
    storageInstructions = storageInstructions,
    reminderMinuteOfDay = reminderMinuteOfDay,
    storageType = storageType?.storageKey,
)
