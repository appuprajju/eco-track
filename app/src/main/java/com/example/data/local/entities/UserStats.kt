package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val email: String, // Associated email as primary key
    val userName: String = "Eco Pioneer",
    val points: Int = 150, // Initial welcome points
    val level: Int = 1,
    val consecutiveLoginStreak: Int = 1,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val totalCo2SavedKg: Double = 0.0,
    val loginTime: Long = System.currentTimeMillis() // Tracks session start
)
