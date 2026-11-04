package com.expirybuddy.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
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
)

fun ExpiryItemEntity.toDomain(): ExpiryItem = ExpiryItem(
    id = id,
    name = name,
    category = ItemCategory.fromStorageKey(category),
    expiryDate = expiryDate,
    notes = notes,
    reminderDaysBefore = reminderDaysBefore,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun ExpiryItem.toEntity(): ExpiryItemEntity = ExpiryItemEntity(
    id = id,
    name = name,
    category = category.storageKey,
    expiryDate = expiryDate,
    notes = notes,
    reminderDaysBefore = reminderDaysBefore,
    createdAt = createdAt.toEpochMilli(),
)
