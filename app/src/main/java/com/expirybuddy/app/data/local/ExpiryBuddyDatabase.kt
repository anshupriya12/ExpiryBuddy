package com.expirybuddy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ExpiryItemEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ExpiryBuddyDatabase : RoomDatabase() {
    abstract fun itemDao(): ExpiryItemDao

    companion object {
        const val DATABASE_NAME = "expiry_buddy.db"

        // No fallbackToDestructiveMigration: silently wiping a user's saved items is never
        // acceptable. Schema changes must ship a real Migration.
        fun create(context: Context): ExpiryBuddyDatabase =
            Room.databaseBuilder(context.applicationContext, ExpiryBuddyDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
