package com.expirybuddy.app.data.local

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ExpiryItemEntityMappingTest {

    private val item = ExpiryItem(
        id = 5,
        name = "Amoxicillin",
        category = ItemCategory.MEDICATION,
        expiryDate = LocalDate.of(2026, 8, 1),
        notes = "Keep in fridge",
        reminderDaysBefore = 3,
        createdAt = Instant.ofEpochMilli(1_700_000_000_123L),
    )

    @Test
    fun domainToEntityAndBack_isLossless() {
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun entity_storesCategoryByKeyAndCreatedAtAsMillis() {
        val entity = item.toEntity()
        assertEquals("medication", entity.category)
        assertEquals(1_700_000_000_123L, entity.createdAt)
    }

    @Test
    fun nullNotesAndNoReminder_surviveMapping() {
        val bare = item.copy(notes = null, reminderDaysBefore = null)
        val restored = bare.toEntity().toDomain()
        assertNull(restored.notes)
        assertNull(restored.reminderDaysBefore)
    }

    @Test
    fun unknownCategoryKey_fallsBackToOther() {
        val entity = item.toEntity().copy(category = "from-a-future-version")
        assertEquals(ItemCategory.OTHER, entity.toDomain().category)
    }

    @Test
    fun everyCategory_roundTripsThroughItsKey() {
        ItemCategory.entries.forEach {
            assertEquals(it, ItemCategory.fromStorageKey(it.storageKey))
        }
    }

    @Test
    fun categoryKeys_areUnique() {
        assertEquals(ItemCategory.entries.size, ItemCategory.entries.map { it.storageKey }.toSet().size)
    }
}
