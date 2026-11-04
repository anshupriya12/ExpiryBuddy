package com.expirybuddy.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** All functions are main-safe: Room runs suspend and Flow queries off the main thread. */
@Dao
interface ExpiryItemDao {

    /** Soonest expiry first; id breaks ties so the order is stable. */
    @Query("SELECT * FROM items ORDER BY expiryDate ASC, id ASC")
    fun observeAll(): Flow<List<ExpiryItemEntity>>

    /** One-shot read of everything, used to restore reminders at app start. */
    @Query("SELECT * FROM items ORDER BY expiryDate ASC, id ASC")
    suspend fun getAll(): List<ExpiryItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ExpiryItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ExpiryItemEntity?

    /** Returns the generated row id. */
    @Insert
    suspend fun insert(entity: ExpiryItemEntity): Long

    /** Returns the number of rows updated (0 if the id no longer exists). */
    @Update
    suspend fun update(entity: ExpiryItemEntity): Int

    /** Returns the number of rows deleted. */
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
