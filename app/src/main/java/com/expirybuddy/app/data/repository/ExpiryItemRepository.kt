package com.expirybuddy.app.data.repository

import com.expirybuddy.app.domain.model.ExpiryItem
import kotlinx.coroutines.flow.Flow

/** Single source of truth for saved items. Everything is local; there is no network. */
interface ExpiryItemRepository {

    /** All items, soonest expiry first. Emits again whenever the data changes. */
    fun observeItems(): Flow<List<ExpiryItem>>

    /** Emits null when the item does not exist (for example after it is deleted). */
    fun observeItem(id: Long): Flow<ExpiryItem?>

    suspend fun getItem(id: Long): ExpiryItem?

    /** One-shot snapshot of every item, soonest expiry first. */
    suspend fun getAllItems(): List<ExpiryItem>

    /**
     * Inserts the item when [ExpiryItem.id] is [ExpiryItem.NEW_ID], otherwise updates it.
     * Returns the item's id.
     *
     * @throws ItemNotFoundException when updating an item that no longer exists.
     */
    suspend fun save(item: ExpiryItem): Long

    /** Returns true if an item was deleted, false if it did not exist. */
    suspend fun delete(id: Long): Boolean
}

class ItemNotFoundException(val id: Long) : Exception("No item with id $id")
