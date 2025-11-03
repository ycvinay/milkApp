package com.example.farmermilkcollectionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "milk_collection")
data class MilkCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- farmerId and farmerName are REMOVED ---

    val date: Long = Date().time,
    val session: String,
    val quantityLitres: Double,
    val fatPercentage: Double,
    val pricePerLitre: Double,
    val paymentStatus: String = "Pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() = quantityLitres * pricePerLitre
}