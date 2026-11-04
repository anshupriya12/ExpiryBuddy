package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ItemFilterTest {
    private val item = ExpiryItem(
        id = 1,
        name = "Greek Yogurt",
        category = ItemCategory.FOOD,
        expiryDate = LocalDate.of(2026, 3, 1),
        notes = "Bought at the Market",
        createdAt = Instant.EPOCH,
    )

    @Test
    fun defaultFilter_matchesEverythingAndIsInactive() {
        val filter = ItemFilter()
        assertTrue(filter.matches(item))
        assertFalse(filter.isActive)
    }

    @Test
    fun blankQuery_matchesEverythingAndIsInactive() {
        val filter = ItemFilter(query = "   ")
        assertTrue(filter.matches(item))
        assertFalse(filter.isActive)
    }

    @Test
    fun query_matchesNameIgnoringCase() {
        assertTrue(ItemFilter(query = "yOgUrT").matches(item))
    }

    @Test
    fun query_matchesNotes() {
        assertTrue(ItemFilter(query = "market").matches(item))
    }

    @Test
    fun query_isTrimmed() {
        assertTrue(ItemFilter(query = "  greek ").matches(item))
    }

    @Test
    fun query_withNoMatch_isRejected() {
        assertFalse(ItemFilter(query = "cheese").matches(item))
    }

    @Test
    fun query_onItemWithoutNotes_stillMatchesName() {
        val bare = item.copy(notes = null)
        assertTrue(ItemFilter(query = "greek").matches(bare))
        assertFalse(ItemFilter(query = "market").matches(bare))
    }

    @Test
    fun category_mustMatch() {
        assertTrue(ItemFilter(category = ItemCategory.FOOD).matches(item))
        assertFalse(ItemFilter(category = ItemCategory.MEDICATION).matches(item))
    }

    @Test
    fun queryAndCategory_bothApply() {
        assertTrue(ItemFilter("greek", ItemCategory.FOOD).matches(item))
        assertFalse(ItemFilter("greek", ItemCategory.OTHER).matches(item))
        assertFalse(ItemFilter("cheese", ItemCategory.FOOD).matches(item))
    }

    @Test
    fun filterIsActive_whenQueryOrCategorySet() {
        assertTrue(ItemFilter(query = "x").isActive)
        assertTrue(ItemFilter(category = ItemCategory.FOOD).isActive)
    }
}
