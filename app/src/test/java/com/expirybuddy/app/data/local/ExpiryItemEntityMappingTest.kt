package com.expirybuddy.app.data.local

import com.expirybuddy.app.domain.expiry.UseWithinUnit
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
    fun v2Fields_photoUpdatedAtAndUsedAt_surviveMapping() {
        val v2 = item.copy(
            photoUri = "file:///photos/a.jpg",
            updatedAt = Instant.ofEpochMilli(1_700_000_500_000L),
            usedAt = Instant.ofEpochMilli(1_700_000_900_000L),
        )
        val entity = v2.toEntity()
        assertEquals("file:///photos/a.jpg", entity.photoUri)
        assertEquals(1_700_000_500_000L, entity.updatedAt)
        assertEquals(1_700_000_900_000L, entity.usedAt)
        assertEquals(v2, entity.toDomain())
    }

    @Test
    fun activeItemWithoutPhoto_hasNullUsedAtAndPhoto() {
        val entity = item.toEntity()
        assertNull(entity.photoUri)
        assertNull(entity.usedAt)
        assertEquals(false, entity.toDomain().isUsed)
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

    @Test
    fun v22Fields_openingDetailsStorageAndReminderTime_surviveMapping() {
        val v22 = item.copy(
            openedOn = LocalDate.of(2026, 11, 12),
            useWithinAmount = 2,
            useWithinUnit = UseWithinUnit.MONTHS,
            storageInstructions = "Keep refrigerated",
            reminderMinuteOfDay = 19 * 60 + 30,
        )
        val entity = v22.toEntity()
        assertEquals(LocalDate.of(2026, 11, 12), entity.openedOn)
        assertEquals(2, entity.useWithinAmount)
        assertEquals("months", entity.useWithinUnit)
        assertEquals(19 * 60 + 30, entity.reminderMinuteOfDay)
        assertEquals(v22, entity.toDomain())
    }

    @Test
    fun anItemWithoutADuration_storesNoUnit_andReadsBackAsDays() {
        val entity = item.toEntity()
        assertNull(entity.useWithinUnit)
        assertNull(entity.openedOn)
        assertNull(entity.reminderMinuteOfDay)
        assertEquals(item, entity.toDomain())
    }
}
