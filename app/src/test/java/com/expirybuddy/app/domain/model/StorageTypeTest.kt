package com.expirybuddy.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageTypeTest {
    @Test
    fun offersExactlyRefrigerateFrozenAndAirtight() {
        assertEquals(
            listOf(StorageType.REFRIGERATE, StorageType.FROZEN, StorageType.AIRTIGHT),
            StorageType.entries,
        )
    }

    @Test
    fun storageKeys_roundTrip() {
        StorageType.entries.forEach { assertEquals(it, StorageType.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun nullOrUnknownKey_meansNotSpecified() {
        assertNull(StorageType.fromStorageKey(null))
        assertNull(StorageType.fromStorageKey("shelf"))
    }
}
