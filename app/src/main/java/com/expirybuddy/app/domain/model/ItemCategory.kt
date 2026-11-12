package com.expirybuddy.app.domain.model

/**
 * Fixed set of categories. Stored by [storageKey], never by ordinal, so reordering or
 * renaming the enum constants cannot corrupt saved data. Display labels live in string resources.
 */
enum class ItemCategory(val storageKey: String) {
    FOOD("food"),
    MEDICATION("medication"),
    COSMETICS("cosmetics"),
    OTHER("other");

    companion object {
        /**
         * Unknown or legacy keys fall back to [OTHER] instead of crashing. This covers the removed
         * "document" and "household" categories: old rows are also rewritten to "other" by
         * `MIGRATION_3_4`, and this fallback keeps reading safe either way.
         */
        fun fromStorageKey(key: String?): ItemCategory =
            entries.firstOrNull { it.storageKey == key } ?: OTHER
    }
}
