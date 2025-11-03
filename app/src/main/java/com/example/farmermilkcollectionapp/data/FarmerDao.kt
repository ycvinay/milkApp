package com.example.farmermilkcollectionapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerDao {

    // Inserts a new farmer. If a farmer with the same ID already exists, it replaces it.


    // (We can add update and delete functions later if needed)
}