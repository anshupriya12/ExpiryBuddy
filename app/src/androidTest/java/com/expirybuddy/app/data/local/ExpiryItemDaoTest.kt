package com.expirybuddy.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs against a real in-memory Room database on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class ExpiryItemDaoTest {

    private lateinit var db: ExpiryBuddyDatabase
    private lateinit var dao: ExpiryItemDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ExpiryBuddyDatabase::class.java).build()
        dao = db.itemDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(name: String, date: LocalDate, notes: String? = null) = ExpiryItemEntity(
        name = name,
        category = "food",
        expiryDate = date,
        notes = notes,
        reminderDaysBefore = null,
        createdAt = 1_000L,
    )

    @Test
    fun insertThenGet_roundTripsAllFields() = runBlocking {
        val id = dao.insert(entity("Yogurt", LocalDate.of(2026, 2, 28), notes = "n").copy(reminderDaysBefore = 3))

        val loaded = dao.getById(id)!!
        assertEquals("Yogurt", loaded.name)
        assertEquals(LocalDate.of(2026, 2, 28), loaded.expiryDate)
        assertEquals("n", loaded.notes)
        assertEquals(3, loaded.reminderDaysBefore)
        assertEquals(1_000L, loaded.createdAt)
    }

    @Test
    fun observeAll_isOrderedByExpiryDateThenId() = runBlocking {
        dao.insert(entity("c", LocalDate.of(2026, 3, 1)))
        dao.insert(entity("a", LocalDate.of(2026, 1, 1)))
        dao.insert(entity("b", LocalDate.of(2026, 1, 1)))

        assertEquals(listOf("a", "b", "c"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun getAll_returnsEverythingInExpiryOrder() = runBlocking {
        dao.insert(entity("b", LocalDate.of(2026, 3, 1)))
        dao.insert(entity("a", LocalDate.of(2026, 1, 1)))

        assertEquals(listOf("a", "b"), dao.getAll().map { it.name })
    }

    @Test
    fun update_returnsRowCount() = runBlocking {
        val id = dao.insert(entity("x", LocalDate.of(2026, 1, 1)))

        assertEquals(1, dao.update(entity("y", LocalDate.of(2026, 1, 2)).copy(id = id)))
        assertEquals("y", dao.getById(id)!!.name)
        assertEquals(0, dao.update(entity("z", LocalDate.of(2026, 1, 2)).copy(id = id + 100)))
    }

    @Test
    fun delete_removesRow() = runBlocking {
        val id = dao.insert(entity("x", LocalDate.of(2026, 1, 1)))

        assertEquals(1, dao.deleteById(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.deleteById(id))
    }
}
