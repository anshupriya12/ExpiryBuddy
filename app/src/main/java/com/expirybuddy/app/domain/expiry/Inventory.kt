package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import java.time.LocalDate

/** How the All Items list is ordered. The default is earliest expiry first. */
enum class ItemSort { EXPIRY_DATE, NAME, CATEGORY }

/**
 * Builds the flat, filtered and sorted list behind the All Items screen. Pure functions, so the
 * ordering rules are unit-testable. Reuses [ItemFilter] and [ExpiryClassifier]; nothing is duplicated.
 */
object InventoryBuilder {
    private val byNameThenId = compareBy<ClassifiedItem, String>(String.CASE_INSENSITIVE_ORDER) { it.item.name }
        .thenBy { it.item.id }
    private val byExpiry = compareBy<ClassifiedItem> { it.item.expiryDate }.then(byNameThenId)

    private val orders = mapOf(
        ItemSort.EXPIRY_DATE to byExpiry,
        ItemSort.NAME to compareBy<ClassifiedItem, String>(String.CASE_INSENSITIVE_ORDER) { it.item.name }
            .thenBy { it.item.expiryDate }
            .thenBy { it.item.id },
        ItemSort.CATEGORY to compareBy<ClassifiedItem> { it.item.category.ordinal }.then(byExpiry),
    )

    /** Active (not used) items matching [filter], in [sort] order. */
    fun active(items: List<ExpiryItem>, filter: ItemFilter, sort: ItemSort, today: LocalDate): List<ClassifiedItem> =
        items.asSequence()
            .filter { !it.isUsed && filter.matches(it) }
            .map { classify(it, today) }
            .sortedWith(orders.getValue(sort))
            .toList()

    /** Items marked as used matching [filter], most recently used first. */
    fun used(items: List<ExpiryItem>, filter: ItemFilter, today: LocalDate): List<ClassifiedItem> =
        items.asSequence()
            .filter { it.isUsed && filter.matches(it) }
            .map { classify(it, today) }
            .sortedWith(compareByDescending<ClassifiedItem> { it.item.usedAt }.then(byNameThenId))
            .toList()

    private fun classify(item: ExpiryItem, today: LocalDate) = ClassifiedItem(
        item = item,
        status = ExpiryClassifier.classify(item.expiryDate, today),
        daysUntilExpiry = ExpiryClassifier.daysUntil(item.expiryDate, today),
    )
}

/** Part of the day, for the Home greeting. */
enum class DayPeriod {
    MORNING, AFTERNOON, EVENING;

    companion object {
        /** 05:00-11:59 morning, 12:00-16:59 afternoon, everything else evening. */
        fun forHour(hour: Int): DayPeriod = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            else -> EVENING
        }
    }
}
