package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import java.time.LocalDate

/** An item together with its status relative to a given "today". */
data class ClassifiedItem(
    val item: ExpiryItem,
    val status: ExpiryStatus,
    /** Negative when already expired, 0 when it expires today. */
    val daysUntilExpiry: Long,
)

data class StatusCounts(
    val expired: Int,
    val expiringSoon: Int,
    val upcoming: Int,
) {
    val total: Int get() = expired + expiringSoon + upcoming
}

/**
 * What the Home list shows. [counts] always cover every saved item; the three lists contain only
 * items that match the active filter.
 */
data class HomeSections(
    val counts: StatusCounts,
    val expired: List<ClassifiedItem>,
    val expiringSoon: List<ClassifiedItem>,
    val upcoming: List<ClassifiedItem>,
) {
    val visibleCount: Int get() = expired.size + expiringSoon.size + upcoming.size
    val isEmpty: Boolean get() = visibleCount == 0
}

object HomeSectionsBuilder {
    // Expired: most recently expired first (closest to today). Others: soonest first.
    // Ties break on name (case-insensitive) then id so the order never jitters.
    private val byNameThenId = compareBy<ClassifiedItem, String>(String.CASE_INSENSITIVE_ORDER) { it.item.name }
        .thenBy { it.item.id }
    private val expiredOrder = compareByDescending<ClassifiedItem> { it.item.expiryDate }.then(byNameThenId)
    private val upcomingOrder = compareBy<ClassifiedItem> { it.item.expiryDate }.then(byNameThenId)

    fun build(items: List<ExpiryItem>, filter: ItemFilter, today: LocalDate): HomeSections {
        val classified = items.map {
            ClassifiedItem(
                item = it,
                status = ExpiryClassifier.classify(it.expiryDate, today),
                daysUntilExpiry = ExpiryClassifier.daysUntil(it.expiryDate, today),
            )
        }
        val counts = StatusCounts(
            expired = classified.count { it.status == ExpiryStatus.EXPIRED },
            expiringSoon = classified.count { it.status == ExpiryStatus.EXPIRING_SOON },
            upcoming = classified.count { it.status == ExpiryStatus.UPCOMING },
        )
        val visible = classified.filter { filter.matches(it.item) }
        return HomeSections(
            counts = counts,
            expired = visible.filter { it.status == ExpiryStatus.EXPIRED }.sortedWith(expiredOrder),
            expiringSoon = visible.filter { it.status == ExpiryStatus.EXPIRING_SOON }.sortedWith(upcomingOrder),
            upcoming = visible.filter { it.status == ExpiryStatus.UPCOMING }.sortedWith(upcomingOrder),
        )
    }
}
