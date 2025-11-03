package com.example.farmermilkcollectionapp.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilkRepository @Inject constructor(
    private val dao: MilkCollectionDao // Changed variable name to dao
) {
    fun getAllCollections() = dao.getAllCollections()
    suspend fun insert(collection: MilkCollection) = dao.insert(collection)
    suspend fun update(collection: MilkCollection) = dao.update(collection)

    // --- 1. FIXED: Changed id to Long ---
    suspend fun delete(id: Int) = dao.deleteById(id)

    // --- 2. ADDED: New functions for Dashboard ---
    fun getCollectionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MilkCollection>> {
        return dao.getCollectionsForDay(startOfDay, endOfDay)
    }

    fun getLatestRecords(): Flow<List<MilkCollection>> {
        return dao.getLatestRecords()
    }
}