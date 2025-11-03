package com.example.farmermilkcollectionapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: MilkCollection)

    @Query("SELECT * FROM milk_collection ORDER BY date DESC")
    fun getAllCollections(): Flow<List<MilkCollection>>

    @Update
    suspend fun update(record: MilkCollection)

    @Query("DELETE FROM milk_collection WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM milk_collection")
    suspend fun clearAll()

    @Query("SELECT * FROM milk_collection WHERE date BETWEEN :startOfDay AND :endOfDay")
    fun getCollectionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MilkCollection>>

    // --- 2. ADD THIS NEW FUNCTION ---
    // Gets the most recent 10 records
    @Query("SELECT * FROM milk_collection ORDER BY date DESC LIMIT 10")
    fun getLatestRecords(): Flow<List<MilkCollection>>
}
