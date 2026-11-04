package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HomeSectionsBuilderTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun item(
        id: Long,
        name: String,
        daysFromToday: Long,
        category: ItemCategory = ItemCategory.FOOD,
        notes: String? = null,
    ) = ExpiryItem(
        id = id,
        name = name,
        category = category,
        expiryDate = today.plusDays(daysFromToday),
        notes = notes,
        createdAt = Instant.EPOCH,
    )

    private fun names(list: List<ClassifiedItem>) = list.map { it.item.name }

    private val sample = listOf(
        item(1, "old milk", -5),
        item(2, "eggs", 3),
        item(3, "passport", 400, ItemCategory.DOCUMENT),
        item(4, "yogurt", 0),
        item(5, "stale bread", -1),
        item(6, "rice", 8),
        item(7, "aspirin", 7, ItemCategory.MEDICATION, notes = "kitchen drawer"),
    )

    @Test
    fun items_areGroupedByStatus() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(), today)

        assertEquals(setOf("old milk", "stale bread"), names(sections.expired).toSet())
        assertEquals(setOf("yogurt", "eggs", "aspirin"), names(sections.expiringSoon).toSet())
        assertEquals(setOf("rice", "passport"), names(sections.upcoming).toSet())
    }

    @Test
    fun counts_matchTheGroups() {
        val counts = HomeSectionsBuilder.build(sample, ItemFilter(), today).counts

        assertEquals(StatusCounts(expired = 2, expiringSoon = 3, upcoming = 2), counts)
        assertEquals(7, counts.total)
    }

    @Test
    fun expired_isMostRecentlyExpiredFirst() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(), today)
        assertEquals(listOf("stale bread", "old milk"), names(sections.expired))
    }

    @Test
    fun expiringSoonAndUpcoming_areSoonestFirst() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(), today)
        assertEquals(listOf("yogurt", "eggs", "aspirin"), names(sections.expiringSoon))
        assertEquals(listOf("rice", "passport"), names(sections.upcoming))
    }

    @Test
    fun sameDate_tiesBreakOnNameIgnoringCaseThenId() {
        val items = listOf(
            item(3, "banana", 2),
            item(1, "Apple", 2),
            item(2, "apple", 2),
        )
        val sections = HomeSectionsBuilder.build(items, ItemFilter(), today)

        assertEquals(listOf(1L, 2L, 3L), sections.expiringSoon.map { it.item.id })
    }

    @Test
    fun daysUntilExpiry_isAttachedToEachItem() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(), today)

        assertEquals(-1L, sections.expired.first().daysUntilExpiry)
        assertEquals(0L, sections.expiringSoon.first().daysUntilExpiry)
        assertEquals(8L, sections.upcoming.first().daysUntilExpiry)
    }

    @Test
    fun searchFilter_narrowsTheListsButNotTheCounts() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(query = "milk"), today)

        assertEquals(listOf("old milk"), names(sections.expired))
        assertTrue(sections.expiringSoon.isEmpty())
        assertTrue(sections.upcoming.isEmpty())
        assertEquals(1, sections.visibleCount)
        assertEquals(7, sections.counts.total)
    }

    @Test
    fun searchFilter_matchesNotes() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(query = "drawer"), today)
        assertEquals(listOf("aspirin"), names(sections.expiringSoon))
    }

    @Test
    fun categoryFilter_keepsOnlyThatCategory() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(category = ItemCategory.DOCUMENT), today)

        assertEquals(listOf("passport"), names(sections.upcoming))
        assertEquals(1, sections.visibleCount)
    }

    @Test
    fun noMatches_isEmptyButCountsRemain() {
        val sections = HomeSectionsBuilder.build(sample, ItemFilter(query = "zzz"), today)

        assertTrue(sections.isEmpty)
        assertEquals(7, sections.counts.total)
    }

    @Test
    fun noItems_isEmptyWithZeroCounts() {
        val sections = HomeSectionsBuilder.build(emptyList(), ItemFilter(), today)

        assertTrue(sections.isEmpty)
        assertEquals(StatusCounts(0, 0, 0), sections.counts)
    }

    @Test
    fun movingTodayForward_reclassifiesItems() {
        val items = listOf(item(1, "yogurt", 1))

        val before = HomeSectionsBuilder.build(items, ItemFilter(), today)
        val after = HomeSectionsBuilder.build(items, ItemFilter(), today.plusDays(2))

        assertEquals(1, before.counts.expiringSoon)
        assertEquals(1, after.counts.expired)
    }
}
