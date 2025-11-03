package com.example.farmermilkcollectionapp.data

import androidx.room.Database
import androidx.room.RoomDatabase

// 1. ADD: Farmer::class to the entities list
// 2. CHANGE: We can keep version = 1, since you uninstalled the app.
@Database(entities = [MilkCollection::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun milkCollectionDao(): MilkCollectionDao

}

// 4. (You will also need to create the FarmerDao interface)