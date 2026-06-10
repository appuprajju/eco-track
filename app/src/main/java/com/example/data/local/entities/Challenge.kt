package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // e.g., "Zero Waste Week"
    val description: String, // info on how to complete it
    val pointsAwarded: Int, // gamified score reward
    val participantsCount: Int, // number of other active users joined
    val difficulty: String, // Easy, Medium, Hard
    val isJoined: Boolean = false,
    val isCompleted: Boolean = false
)
