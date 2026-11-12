package com.expirybuddy.app.domain.model

/**
 * Optional, quick way to say how an item is stored. Stored by [storageKey], never by ordinal.
 * Free-text details live separately in `ExpiryItem.storageInstructions`.
 */
enum class StorageType(val storageKey: String) {
    REFRIGERATE("refrigerate"),
    FROZEN("frozen"),
    AIRTIGHT("airtight"),
    ;

    companion object {
        /** Null or an unknown key means "not specified", never an error. */
        fun fromStorageKey(key: String?): StorageType? = entries.firstOrNull { it.storageKey == key }
    }
}
