package com.example.data.local.daos

import androidx.room.*
import com.example.data.local.entities.CarbonLog
import com.example.data.local.entities.Challenge
import com.example.data.local.entities.Goal
import com.example.data.local.entities.UserStats
import com.example.data.local.entities.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface EcoTrackDao {

    // --- User Accounts DAO Operations ---
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccount(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount)

    // --- Carbon Log DAO Operations ---
    @Query("SELECT * FROM carbon_logs WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getAllCarbonLogs(userEmail: String): Flow<List<CarbonLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarbonLog(log: CarbonLog): Long

    @Query("DELETE FROM carbon_logs WHERE id = :id")
    suspend fun deleteCarbonLogById(id: Int)

    @Query("SELECT SUM(carbonCo2Kg) FROM carbon_logs WHERE userEmail = :userEmail")
    fun getTotalEmissionsFlow(userEmail: String): Flow<Double?>

    // --- Goal DAO Operations ---
    @Query("SELECT * FROM goals WHERE userEmail = :userEmail ORDER BY deadlineTimestamp ASC")
    fun getAllGoals(userEmail: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Query("UPDATE goals SET currentCo2SavedKg = :progress, isCompleted = :completed WHERE id = :id")
    suspend fun updateGoalProgress(id: Int, progress: Double, completed: Boolean)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // --- Challenge DAO Operations ---
    @Query("SELECT * FROM challenges")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<Challenge>)

    @Query("UPDATE challenges SET isJoined = :joined WHERE id = :id")
    suspend fun updateChallengeJoinState(id: Int, joined: Boolean)

    @Query("UPDATE challenges SET isCompleted = :completed WHERE id = :id")
    suspend fun updateChallengeCompletionState(id: Int, completed: Boolean)

    @Query("SELECT COUNT(*) FROM challenges WHERE isCompleted = 1")
    fun getCompletedChallengesCountFlow(): Flow<Int>

    // --- User Stats DAO Operations ---
    @Query("SELECT * FROM user_stats WHERE email = :email LIMIT 1")
    fun getUserStatsFlow(email: String): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStats)

    @Query("UPDATE user_stats SET points = points + :pointsAdded WHERE email = :email")
    suspend fun addUserPoints(email: String, pointsAdded: Int)
}
