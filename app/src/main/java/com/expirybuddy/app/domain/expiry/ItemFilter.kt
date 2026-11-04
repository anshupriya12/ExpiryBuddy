package com.expirybuddy.app.domain.expiry

import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory

/**
 * Search text plus an optional category. The query matches the item's name or notes, ignoring
 * case and surrounding whitespace; a blank query matches everything.
 */
data class ItemFilter(
    val query: String = "",
    val category: ItemCategory? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || category != null

    fun matches(item: ExpiryItem): Boolean {
        if (category != null && item.category != category) return false
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return item.name.contains(needle, ignoreCase = true) ||
            item.notes?.contains(needle, ignoreCase = true) == true
    }
}
