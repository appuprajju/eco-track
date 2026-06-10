package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [
        androidx.room.Index(value = ["deadlineTimestamp"]),
        androidx.room.Index(value = ["userEmail"])
    ]
)
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String, // Track which user set this goal
    val title: String, // e.g. "Ride bike to work"
    val targetCo2ReductionKg: Double, // target emission reduction (e.g. 50 kg)
    val currentCo2SavedKg: Double, // savings accumulated so far (e.g. 15 kg)
    val category: String, // TRANSPORT, ENERGY, etc.
    val deadlineTimestamp: Long, // when the target should be hit
    val isCompleted: Boolean = false
)
