package com.expirybuddy.app.data.repository

import com.expirybuddy.app.data.local.ExpiryItemDao
import com.expirybuddy.app.data.local.ExpiryItemEntity
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Exercises the repository's own logic against an in-memory fake DAO (no Android needed). */
class RoomExpiryItemRepositoryTest {

    private val dao = FakeItemDao()
    private val repository = RoomExpiryItemRepository(dao)

    private fun newItem(name: String = "Milk", date: LocalDate = LocalDate.of(2026, 5, 1)) = ExpiryItem(
        name = name,
        category = ItemCategory.FOOD,
        expiryDate = date,
        reminderDaysBefore = 2,
        createdAt = Instant.ofEpochMilli(1_000L),
    )

    @Test
    fun save_newItem_assignsIdAndStoresIt() = runTest {
        val id = repository.save(newItem())

        assertTrue(id > 0)
        assertEquals(newItem().copy(id = id), repository.getItem(id))
    }

    @Test
    fun save_existingItem_updatesInsteadOfDuplicating() = runTest {
        val id = repository.save(newItem())
        repository.save(repository.getItem(id)!!.copy(name = "Oat milk"))

        val all = repository.observeItems().first()
        assertEquals(1, all.size)
        assertEquals("Oat milk", all.single().name)
        assertEquals(id, all.single().id)
    }

    @Test
    fun save_updatePreservesCreatedAt() = runTest {
        val id = repository.save(newItem())
        repository.save(repository.getItem(id)!!.copy(name = "Changed"))

        assertEquals(Instant.ofEpochMilli(1_000L), repository.getItem(id)!!.createdAt)
    }

    @Test
    fun save_updateOfMissingItem_throwsItemNotFound() = runTest {
        val ghost = newItem().copy(id = 999)
        val error = runCatching { repository.save(ghost) }.exceptionOrNull()

        assertTrue(error is ItemNotFoundException)
        assertEquals(999L, (error as ItemNotFoundException).id)
        assertTrue(repository.observeItems().first().isEmpty())
    }

    @Test
    fun delete_removesItem_andReportsWhetherItExisted() = runTest {
        val id = repository.save(newItem())

        assertTrue(repository.delete(id))
        assertNull(repository.getItem(id))
        assertFalse(repository.delete(id))
    }

    @Test
    fun observeItems_isSortedBySoonestExpiry() = runTest {
        repository.save(newItem("late", LocalDate.of(2026, 9, 1)))
        repository.save(newItem("early", LocalDate.of(2026, 1, 1)))
        repository.save(newItem("middle", LocalDate.of(2026, 5, 1)))

        assertEquals(listOf("early", "middle", "late"), repository.observeItems().first().map { it.name })
    }

    @Test
    fun observeItem_emitsNullAfterDelete() = runTest {
        val id = repository.save(newItem())
        assertEquals("Milk", repository.observeItem(id).first()?.name)

        repository.delete(id)

        assertNull(repository.observeItem(id).first())
    }
}

private class FakeItemDao : ExpiryItemDao {
    private val rows = MutableStateFlow<List<ExpiryItemEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ExpiryItemEntity>> =
        rows.map { list -> list.sortedWith(compareBy({ it.expiryDate }, { it.id })) }

    override fun observeById(id: Long): Flow<ExpiryItemEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): ExpiryItemEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<ExpiryItemEntity> =
        rows.value.sortedWith(compareBy({ it.expiryDate }, { it.id }))

    override suspend fun insert(entity: ExpiryItemEntity): Long {
        val saved = entity.copy(id = nextId++)
        rows.value = rows.value + saved
        return saved.id
    }

    override suspend fun update(entity: ExpiryItemEntity): Int {
        if (rows.value.none { it.id == entity.id }) return 0
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == id }
        return before - rows.value.size
    }
}
