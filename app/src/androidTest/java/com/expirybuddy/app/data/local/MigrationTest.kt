package com.expirybuddy.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.expirybuddy.app.domain.model.ItemCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Proves the V2 redesign does not lose anyone's saved items: a real version 1 database (built from the
 * exported schema) is migrated to version 2 and its rows must come through unchanged.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExpiryBuddyDatabase::class.java,
    )

    private val epochDay = LocalDate.of(2026, 8, 1).toEpochDay()

    private fun createVersion1WithTwoItems(name: String) {
        helper.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO items (id, name, category, expiryDate, notes, reminderDaysBefore, createdAt) " +
                    "VALUES (1, 'Milk', 'food', $epochDay, 'cold shelf', 3, 1700000000123)",
            )
            execSQL(
                "INSERT INTO items (id, name, category, expiryDate, notes, reminderDaysBefore, createdAt) " +
                    "VALUES (2, 'Passport', 'document', ${epochDay + 400}, NULL, NULL, 1700000999000)",
            )
            close()
        }
    }

    @Test
    fun migrate1To2_keepsEveryRow_andAddsTheNewColumnsWithSafeDefaults() {
        createVersion1WithTwoItems(TEST_DB)

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ExpiryBuddyDatabase.MIGRATION_1_2)

        db.query("SELECT id, name, category, expiryDate, notes, reminderDaysBefore, createdAt, photoUri, updatedAt, usedAt FROM items ORDER BY id").use { c ->
            assertEquals(2, c.count)

            c.moveToFirst()
            assertEquals("Milk", c.getString(1))
            assertEquals("food", c.getString(2))
            assertEquals(epochDay, c.getLong(3))
            assertEquals("cold shelf", c.getString(4))
            assertEquals(3, c.getInt(5))
            assertEquals(1700000000123L, c.getLong(6))
            assertNull("no photo yet", if (c.isNull(7)) null else c.getString(7))
            assertEquals("updatedAt starts as createdAt", 1700000000123L, c.getLong(8))
            assertEquals("still active", true, c.isNull(9))

            c.moveToNext()
            assertEquals("Passport", c.getString(1))
            assertEquals(true, c.isNull(4))
            assertEquals(true, c.isNull(5))
            assertEquals(1700000999000L, c.getLong(8))
        }
    }

    @Test
    fun migratedDatabase_opensWithRoom_andOldItemsReadAsActiveItemsWithoutPhotos() = runBlocking {
        createVersion1WithTwoItems(TEST_DB)
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db = Room.databaseBuilder(context, ExpiryBuddyDatabase::class.java, TEST_DB)
            .addMigrations(ExpiryBuddyDatabase.MIGRATION_1_2)
            .build()
        try {
            val milk = db.itemDao().getById(1)!!.toDomain()
            assertEquals("Milk", milk.name)
            assertEquals(ItemCategory.FOOD, milk.category)
            assertEquals(LocalDate.of(2026, 8, 1), milk.expiryDate)
            assertEquals(3, milk.reminderDaysBefore)
            assertNull(milk.photoUri)
            assertFalse(milk.isUsed)
            assertEquals(milk.createdAt, milk.updatedAt)
            assertEquals(2, db.itemDao().getAll().size)
        } finally {
            db.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
