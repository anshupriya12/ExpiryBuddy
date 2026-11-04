package com.expirybuddy.app.testing

import com.expirybuddy.app.data.repository.ExpiryItemRepository
import com.expirybuddy.app.data.repository.ItemNotFoundException
import com.expirybuddy.app.domain.model.ExpiryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** In-memory repository for ViewModel tests, with switches to simulate failures. */
class FakeExpiryItemRepository(initial: List<ExpiryItem> = emptyList()) : ExpiryItemRepository {
    private val items = MutableStateFlow(initial.associateBy { it.id })
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    var failObserve = false
    var failGet = false
    var failSave = false
    var failDelete = false

    val saved: List<ExpiryItem> get() = items.value.values.toList()

    override fun observeItems(): Flow<List<ExpiryItem>> =
        if (failObserve) {
            flow { throw IOException("observe failed") }
        } else {
            items.map { map -> map.values.sortedWith(compareBy({ it.expiryDate }, { it.id })) }
        }

    override fun observeItem(id: Long): Flow<ExpiryItem?> =
        if (failObserve) flow { throw IOException("observe failed") } else items.map { it[id] }

    override suspend fun getItem(id: Long): ExpiryItem? {
        if (failGet) throw IOException("get failed")
        return items.value[id]
    }

    override suspend fun getAllItems(): List<ExpiryItem> =
        items.value.values.sortedWith(compareBy({ it.expiryDate }, { it.id }))

    override suspend fun save(item: ExpiryItem): Long {
        if (failSave) throw IOException("save failed")
        if (item.id == ExpiryItem.NEW_ID) {
            val saved = item.copy(id = nextId++)
            items.value = items.value + (saved.id to saved)
            return saved.id
        }
        if (item.id !in items.value) throw ItemNotFoundException(item.id)
        items.value = items.value + (item.id to item)
        return item.id
    }

    override suspend fun delete(id: Long): Boolean {
        if (failDelete) throw IOException("delete failed")
        val existed = id in items.value
        items.value = items.value - id
        return existed
    }
}
