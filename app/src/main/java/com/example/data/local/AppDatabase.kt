package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.daos.EcoTrackDao
import com.example.data.local.entities.CarbonLog
import com.example.data.local.entities.Challenge
import com.example.data.local.entities.Goal
import com.example.data.local.entities.UserStats
import com.example.data.local.entities.UserAccount

@Database(
    entities = [
        CarbonLog::class,
        Goal::class,
        Challenge::class,
        UserStats::class,
        UserAccount::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ecoTrackDao(): EcoTrackDao
}
