package com.expirybuddy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ExpiryItemEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ExpiryBuddyDatabase : RoomDatabase() {
    abstract fun itemDao(): ExpiryItemDao

    companion object {
        const val DATABASE_NAME = "expiry_buddy.db"

        /**
         * Version 1 -> 2 (V2 redesign): photo, last-updated time and "used" state. Existing rows are
         * kept as they are: no photo, still active, and `updatedAt` starts as the creation time.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN photoUri TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN usedAt INTEGER")
                db.execSQL("UPDATE items SET updatedAt = createdAt")
            }
        }

        /**
         * Version 2 -> 3 (V2.2, use-by tracking and notification preferences): optional opening date,
         * "use within" duration, storage instructions and a per-item reminder time. All columns are
         * nullable and nothing existing is touched: `expiryDate` keeps its values and simply now means
         * the use-by date, and a null reminder time means "follow the default" (9:00 until changed).
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN openedOn INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN useWithinAmount INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN useWithinUnit TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN storageInstructions TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN reminderMinuteOfDay INTEGER")
            }
        }

        // No fallbackToDestructiveMigration: silently wiping a user's saved items is never
        // acceptable. Schema changes must ship a real Migration.
        fun create(context: Context): ExpiryBuddyDatabase =
            Room.databaseBuilder(context.applicationContext, ExpiryBuddyDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
