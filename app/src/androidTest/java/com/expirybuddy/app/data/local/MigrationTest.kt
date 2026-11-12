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
 * Proves schema upgrades never lose anyone's saved items: real version 1 and 2 databases (built from the
 * exported schemas) are migrated forward and their rows must come through unchanged.
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
            .addMigrations(ExpiryBuddyDatabase.MIGRATION_1_2, ExpiryBuddyDatabase.MIGRATION_2_3)
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

    // --- Version 2 -> 3 (V2.2) --------------------------------------------------------------------

    private fun createVersion2WithItems(name: String) {
        helper.createDatabase(name, 2).apply {
            execSQL(
                "INSERT INTO items (id, name, category, expiryDate, notes, reminderDaysBefore, createdAt, photoUri, updatedAt, usedAt) " +
                    "VALUES (1, 'Paneer', 'food', $epochDay, 'fresh', 1, 1700000000123, 'file:///photos/p.jpg', 1700000500000, NULL)",
            )
            execSQL(
                "INSERT INTO items (id, name, category, expiryDate, notes, reminderDaysBefore, createdAt, photoUri, updatedAt, usedAt) " +
                    "VALUES (2, 'Old cream', 'cosmetics', ${epochDay - 30}, NULL, NULL, 1700000999000, NULL, 1700000999000, 1700001999000)",
            )
            close()
        }
    }

    @Test
    fun migrate2To3_keepsEveryExistingValue_andAddsNullableColumns() {
        createVersion2WithItems(TEST_DB)

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, ExpiryBuddyDatabase.MIGRATION_2_3)

        db.query(
            "SELECT id, name, category, expiryDate, notes, reminderDaysBefore, createdAt, photoUri, updatedAt, usedAt, " +
                "openedOn, useWithinAmount, useWithinUnit, storageInstructions, reminderMinuteOfDay FROM items ORDER BY id",
        ).use { c ->
            assertEquals(2, c.count)

            c.moveToFirst()
            assertEquals("Paneer", c.getString(1))
            assertEquals("food", c.getString(2))
            assertEquals("the old date is now the use-by date, unchanged", epochDay, c.getLong(3))
            assertEquals("fresh", c.getString(4))
            assertEquals("reminder choice kept", 1, c.getInt(5))
            assertEquals(1700000000123L, c.getLong(6))
            assertEquals("photo kept", "file:///photos/p.jpg", c.getString(7))
            assertEquals(1700000500000L, c.getLong(8))
            assertEquals(true, c.isNull(9))
            for (col in 10..14) assertEquals("new column $col starts empty", true, c.isNull(col))

            c.moveToNext()
            assertEquals("Old cream", c.getString(1))
            assertEquals("used state kept", 1700001999000L, c.getLong(9))
            assertEquals(true, c.isNull(5))
            for (col in 10..14) assertEquals(true, c.isNull(col))
        }
    }

    @Test
    fun migrate1To3_runsBothStepsInOrder() {
        createVersion1WithTwoItems(TEST_DB)

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 3, true, ExpiryBuddyDatabase.MIGRATION_1_2, ExpiryBuddyDatabase.MIGRATION_2_3,
        )

        db.query("SELECT name, reminderDaysBefore, updatedAt, reminderMinuteOfDay FROM items ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("Milk", c.getString(0))
            assertEquals(3, c.getInt(1))
            assertEquals(1700000000123L, c.getLong(2))
            assertEquals(true, c.isNull(3))
        }
    }

    @Test
    fun migratedV2Database_opensWithRoom_oldItemsFollowTheDefaultTime_andNewFieldsCanBeSaved() = runBlocking {
        createVersion2WithItems(TEST_DB)
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db = Room.databaseBuilder(context, ExpiryBuddyDatabase::class.java, TEST_DB)
            .addMigrations(
                ExpiryBuddyDatabase.MIGRATION_1_2,
                ExpiryBuddyDatabase.MIGRATION_2_3,
            )
            .build()
        try {
            val paneer = db.itemDao().getById(1)!!.toDomain()
            assertEquals(LocalDate.of(2026, 8, 1), paneer.expiryDate)
            assertEquals(1, paneer.reminderDaysBefore)
            assertEquals("file:///photos/p.jpg", paneer.photoUri)
            assertNull("follows the default notification time", paneer.reminderMinuteOfDay)
            assertNull(paneer.openedOn)
            assertNull(paneer.storageInstructions)
            assertEquals(true, db.itemDao().getById(2)!!.toDomain().isUsed)

            val updated = paneer.copy(
                openedOn = LocalDate.of(2026, 7, 30),
                useWithinAmount = 2,
                storageInstructions = "Fridge",
                reminderMinuteOfDay = 19 * 60,
            )
            db.itemDao().update(updated.toEntity())
            val again = db.itemDao().getById(1)!!.toDomain()
            assertEquals(updated, again)
        } finally {
            db.close()
            context.deleteDatabase(TEST_DB)
        }
    }
}
