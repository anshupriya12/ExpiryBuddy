package com.expirybuddy.app.domain.model

/**
 * Fixed set of categories. Stored by [storageKey], never by ordinal, so reordering or
 * renaming the enum constants cannot corrupt saved data. Display labels live in string resources.
 */
enum class ItemCategory(val storageKey: String) {
    FOOD("food"),
    MEDICATION("medication"),
    COSMETICS("cosmetics"),
    DOCUMENT("document"),
    HOUSEHOLD("household"),
    OTHER("other");

    companion object {
        /** Unknown or legacy keys fall back to [OTHER] instead of crashing. */
        fun fromStorageKey(key: String?): ItemCategory =
            entries.firstOrNull { it.storageKey == key } ?: OTHER
    }
}
