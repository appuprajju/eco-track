package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "carbon_logs",
    indices = [
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["category"]),
        androidx.room.Index(value = ["userEmail"])
    ]
)
data class CarbonLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String, // Track which user logged this
    val category: String, // TRANSPORT, ENERGY, FOOD, CONSUMPTION
    val note: String, // Description (e.g., "Commuted by Diesel Car")
    val rawValue: Double, // The numeric amount user logged (e.g. 20.0)
    val unit: String, // km, kWh, meals, grams
    val carbonCo2Kg: Double, // Calculated emissions in kg of CO2
    val timestamp: Long = System.currentTimeMillis()
)
