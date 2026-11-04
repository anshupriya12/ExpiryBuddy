package com.expirybuddy.app.data.repository

import com.expirybuddy.app.data.local.ExpiryItemDao
import com.expirybuddy.app.data.local.toDomain
import com.expirybuddy.app.data.local.toEntity
import com.expirybuddy.app.domain.model.ExpiryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExpiryItemRepository(
    private val dao: ExpiryItemDao,
) : ExpiryItemRepository {

    override fun observeItems(): Flow<List<ExpiryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeItem(id: Long): Flow<ExpiryItem?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getItem(id: Long): ExpiryItem? = dao.getById(id)?.toDomain()

    override suspend fun getAllItems(): List<ExpiryItem> = dao.getAll().map { it.toDomain() }

    override suspend fun save(item: ExpiryItem): Long {
        if (item.id == ExpiryItem.NEW_ID) {
            return dao.insert(item.toEntity())
        }
        val updated = dao.update(item.toEntity())
        if (updated == 0) throw ItemNotFoundException(item.id)
        return item.id
    }

    override suspend fun delete(id: Long): Boolean = dao.deleteById(id) > 0
}
