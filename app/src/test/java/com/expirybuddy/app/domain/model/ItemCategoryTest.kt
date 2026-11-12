package com.expirybuddy.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemCategoryTest {
    @Test
    fun supportedCategories_areFoodMedicationCosmeticsAndOther() {
        assertEquals(
            listOf(ItemCategory.FOOD, ItemCategory.MEDICATION, ItemCategory.COSMETICS, ItemCategory.OTHER),
            ItemCategory.entries,
        )
    }

    @Test
    fun storageKeys_roundTrip() {
        ItemCategory.entries.forEach { assertEquals(it, ItemCategory.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun retiredCategories_documentAndHousehold_readAsOther() {
        assertEquals(ItemCategory.OTHER, ItemCategory.fromStorageKey("document"))
        assertEquals(ItemCategory.OTHER, ItemCategory.fromStorageKey("household"))
    }

    @Test
    fun unknownOrMissingKey_fallsBackToOther() {
        assertEquals(ItemCategory.OTHER, ItemCategory.fromStorageKey(null))
        assertEquals(ItemCategory.OTHER, ItemCategory.fromStorageKey(""))
        assertEquals(ItemCategory.OTHER, ItemCategory.fromStorageKey("pets"))
    }
}
