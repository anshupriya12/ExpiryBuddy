package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class InventoryBuilderTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun item(
        id: Long,
        name: String,
        daysFromToday: Long,
        category: ItemCategory = ItemCategory.FOOD,
        notes: String? = null,
        usedAt: Instant? = null,
    ) = ExpiryItem(
        id = id,
        name = name,
        category = category,
        expiryDate = today.plusDays(daysFromToday),
        notes = notes,
        createdAt = Instant.EPOCH,
        usedAt = usedAt,
    )

    private val items = listOf(
        item(1, "Yogurt", 5),
        item(2, "aspirin", 100, ItemCategory.MEDICATION),
        item(3, "Bread", -2),
        item(4, "Lipstick", 30, ItemCategory.COSMETICS),
        item(5, "Cheese", 5),
    )

    private fun names(sort: ItemSort, filter: ItemFilter = ItemFilter()) =
        InventoryBuilder.active(items, filter, sort, today).map { it.item.name }

    @Test
    fun defaultSort_isEarliestExpiryFirst_includingExpiredOnTop_tiesByName() {
        assertEquals(
            listOf("Bread", "Cheese", "Yogurt", "Lipstick", "aspirin"),
            names(ItemSort.EXPIRY_DATE),
        )
    }

    @Test
    fun nameSort_isCaseInsensitiveAtoZ() {
        assertEquals(
            listOf("aspirin", "Bread", "Cheese", "Lipstick", "Yogurt"),
            names(ItemSort.NAME),
        )
    }

    @Test
    fun categorySort_followsCategoryOrder_thenExpiry() {
        // FOOD (Bread, Cheese, Yogurt by expiry then name), MEDICATION, COSMETICS.
        assertEquals(
            listOf("Bread", "Cheese", "Yogurt", "aspirin", "Lipstick"),
            names(ItemSort.CATEGORY),
        )
    }

    @Test
    fun searchAndCategoryFilter_combine() {
        // "e" appears in Bread, Cheese and Lipstick; restricting to FOOD leaves Bread and Cheese.
        assertEquals(
            listOf("Bread", "Cheese"),
            names(ItemSort.EXPIRY_DATE, ItemFilter(query = "e", category = ItemCategory.FOOD)),
        )
        // "a" appears in Bread and aspirin; MEDICATION leaves only aspirin.
        assertEquals(
            listOf("aspirin"),
            names(ItemSort.EXPIRY_DATE, ItemFilter(query = "a", category = ItemCategory.MEDICATION)),
        )
        // Category alone, and query alone, each still work.
        assertEquals(listOf("Lipstick"), names(ItemSort.NAME, ItemFilter(category = ItemCategory.COSMETICS)))
        assertEquals(listOf("Yogurt"), names(ItemSort.NAME, ItemFilter(query = "yog")))
    }

    @Test
    fun filterMatchesNotes_andNoMatchGivesAnEmptyList() {
        val withNotes = items + item(6, "Tin", 1, notes = "for the picnic")
        assertEquals(
            listOf("Tin"),
            InventoryBuilder.active(withNotes, ItemFilter(query = "picnic"), ItemSort.NAME, today).map { it.item.name },
        )
        assertTrue(InventoryBuilder.active(items, ItemFilter(query = "zzz"), ItemSort.NAME, today).isEmpty())
    }

    @Test
    fun rows_carryStatusAndDayCount() {
        val bread = InventoryBuilder.active(items, ItemFilter(), ItemSort.NAME, today).first { it.item.name == "Bread" }
        assertEquals(ExpiryStatus.EXPIRED, bread.status)
        assertEquals(-2L, bread.daysUntilExpiry)
    }

    @Test
    fun usedItems_areNeverInTheActiveList_andViceVersa() {
        val used = item(7, "Old jam", 10, usedAt = Instant.parse("2026-03-01T00:00:00Z"))
        val all = items + used

        assertTrue(InventoryBuilder.active(all, ItemFilter(), ItemSort.NAME, today).none { it.item.isUsed })
        assertEquals(listOf("Old jam"), InventoryBuilder.used(all, ItemFilter(), today).map { it.item.name })
    }

    @Test
    fun usedList_isMostRecentlyUsedFirst_andRespectsFilters() {
        val a = item(8, "A", 1, usedAt = Instant.parse("2026-03-01T00:00:00Z"))
        val b = item(9, "B", 1, ItemCategory.MEDICATION, usedAt = Instant.parse("2026-03-10T00:00:00Z"))

        assertEquals(listOf("B", "A"), InventoryBuilder.used(listOf(a, b), ItemFilter(), today).map { it.item.name })
        assertEquals(
            listOf("A"),
            InventoryBuilder.used(listOf(a, b), ItemFilter(category = ItemCategory.FOOD), today).map { it.item.name },
        )
    }

    @Test
    fun dayPeriod_boundaries() {
        assertEquals(DayPeriod.EVENING, DayPeriod.forHour(0))
        assertEquals(DayPeriod.EVENING, DayPeriod.forHour(4))
        assertEquals(DayPeriod.MORNING, DayPeriod.forHour(5))
        assertEquals(DayPeriod.MORNING, DayPeriod.forHour(11))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.forHour(12))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.forHour(16))
        assertEquals(DayPeriod.EVENING, DayPeriod.forHour(17))
        assertEquals(DayPeriod.EVENING, DayPeriod.forHour(23))
    }
}
