package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.daos.EcoTrackDao
import com.example.data.local.entities.CarbonLog
import com.example.data.local.entities.Challenge
import com.example.data.local.entities.Goal
import com.example.data.local.entities.UserStats
import com.example.data.local.entities.UserAccount
import com.example.data.remote.Content
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class EcoTrackRepository(
    private val ecoTrackDao: EcoTrackDao,
    private val geminiApiService: GeminiApiService
) {

    // --- Flow Streams (Partitioned by User Email where appropriate) ---
    fun getAllCarbonLogs(email: String): Flow<List<CarbonLog>> = ecoTrackDao.getAllCarbonLogs(email)
    fun getAllGoals(email: String): Flow<List<Goal>> = ecoTrackDao.getAllGoals(email)
    fun getUserStatsFlow(email: String): Flow<UserStats?> = ecoTrackDao.getUserStatsFlow(email)
    fun getTotalEmissionsFlow(email: String): Flow<Double?> = ecoTrackDao.getTotalEmissionsFlow(email)

    // Global Collections
    val allChallenges: Flow<List<Challenge>> = ecoTrackDao.getAllChallenges()
    val completedChallengesCount: Flow<Int> = ecoTrackDao.getCompletedChallengesCountFlow()

    // --- Authentication, Account Creation & Welcome Email ---
    suspend fun getUserAccount(email: String): UserAccount? = withContext(Dispatchers.IO) {
        ecoTrackDao.getUserAccount(email)
    }

    suspend fun registerUser(email: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val existing = ecoTrackDao.getUserAccount(email)
        if (existing != null) {
            return@withContext false // Block duplicate email creation
        }

        // 1. Persist new user account credentials
        val newUser = UserAccount(email = email, name = name, welcomeEmailSent = true)
        ecoTrackDao.insertUserAccount(newUser)

        // 2. Initialize stats with welcome reward points
        getOrCreateUserStats(email, name)

        // 3. Dispatch a Welcome Email via simulated external worker
        simulateSendWelcomeEmail(email, name)

        true
    }

    private fun simulateSendWelcomeEmail(email: String, name: String) {
        println("=========================================================")
        println("✉️ SIMULATED WELCOME EMAIL SENT:")
        println("Destination: $email")
        println("Recipient Name: $name")
        println("Subject: Welcome to EcoTrack AI — Your Carbon Intelligence Partner")
        println("Content: Welcome aboard! We have active premium carbon tracking logs initialized.")
        println("=========================================================")
    }

    suspend fun getOrCreateUserStats(email: String, userName: String): UserStats = withContext(Dispatchers.IO) {
        val current = ecoTrackDao.getUserStatsFlow(email).firstOrNull()
        if (current == null) {
            val stats = UserStats(
                email = email,
                userName = userName,
                points = 150, // Welcome gift points
                level = 1,
                consecutiveLoginStreak = 1,
                lastActiveTimestamp = System.currentTimeMillis(),
                loginTime = System.currentTimeMillis()
            )
            ecoTrackDao.insertOrUpdateUserStats(stats)
            stats
        } else {
            // Refresh login/session start timing
            val updated = current.copy(
                loginTime = System.currentTimeMillis(),
                userName = if (userName.isNotEmpty()) userName else current.userName
            )
            ecoTrackDao.insertOrUpdateUserStats(updated)
            updated
        }
    }

    suspend fun insertDefaultChallenges() = withContext(Dispatchers.IO) {
        val existing = ecoTrackDao.getAllChallenges().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                Challenge(
                    title = "Public Transit Champ",
                    description = "Take bus, metro, or train instead of driving for 3 consecutive days.",
                    pointsAwarded = 100,
                    participantsCount = 1420,
                    difficulty = "Medium"
                ),
                Challenge(
                    title = "Green Diet Shift",
                    description = "Eat vegetarian or vegan meals exclusively for one day to reduce agricultural nitrogen.",
                    pointsAwarded = 75,
                    participantsCount = 2033,
                    difficulty = "Easy"
                ),
                Challenge(
                    title = "Zero Vampire Draw",
                    description = "Unplug all electronics and power strips before bedtime for a clean 100% savings.",
                    pointsAwarded = 50,
                    participantsCount = 890,
                    difficulty = "Easy"
                ),
                Challenge(
                    title = "Cycle Commuter",
                    description = "Commute purely by bicycle or walking for a total distance of 15km.",
                    pointsAwarded = 250,
                    participantsCount = 312,
                    difficulty = "Hard"
                ),
                Challenge(
                    title = "No Plastics Day",
                    description = "Avoid buying single-use plastics or packaged commodities for 24 hours.",
                    pointsAwarded = 60,
                    participantsCount = 1205,
                    difficulty = "Easy"
                )
            )
            ecoTrackDao.insertChallenges(defaults)
        }
    }

    // --- Mutation Operations ---
    suspend fun logActivity(
        email: String,
        category: String,
        note: String,
        rawValue: Double,
        unit: String,
        carbonCo2Kg: Double
    ): Long = withContext(Dispatchers.IO) {
        val log = CarbonLog(
            userEmail = email,
            category = category,
            note = note,
            rawValue = rawValue,
            unit = unit,
            carbonCo2Kg = carbonCo2Kg
        )
        val id = ecoTrackDao.insertCarbonLog(log)

        ecoTrackDao.addUserPoints(email, 15) // reward tracking activity
        updateStreakAndStats(email)
        id
    }

    private suspend fun updateStreakAndStats(email: String) {
        val currentStats = ecoTrackDao.getUserStatsFlow(email).firstOrNull() ?: return
        val now = System.currentTimeMillis()
        val differenceMs = now - currentStats.lastActiveTimestamp
        val differenceDays = differenceMs / (1000 * 60 * 60 * 24)

        var newStreak = currentStats.consecutiveLoginStreak
        if (differenceDays in 1..2) {
            newStreak += 1
        } else if (differenceDays > 2) {
            newStreak = 1
        }

        val newLevel = (currentStats.points / 250).coerceAtLeast(1)

        ecoTrackDao.insertOrUpdateUserStats(
            currentStats.copy(
                consecutiveLoginStreak = newStreak,
                lastActiveTimestamp = now,
                level = newLevel
            )
        )
    }

    suspend fun deleteLog(id: Int) = withContext(Dispatchers.IO) {
        ecoTrackDao.deleteCarbonLogById(id)
    }

    suspend fun addGoal(goal: Goal) = withContext(Dispatchers.IO) {
        ecoTrackDao.insertGoal(goal)
        ecoTrackDao.addUserPoints(goal.userEmail, 20)
    }

    suspend fun deleteGoal(id: Int) = withContext(Dispatchers.IO) {
        ecoTrackDao.deleteGoalById(id)
    }

    suspend fun updateGoalProgress(id: Int, email: String, progress: Double, completed: Boolean) = withContext(Dispatchers.IO) {
        ecoTrackDao.updateGoalProgress(id, progress, completed)
        if (completed) {
            ecoTrackDao.addUserPoints(email, 100)
        }
    }

    suspend fun joinChallenge(id: Int) = withContext(Dispatchers.IO) {
        ecoTrackDao.updateChallengeJoinState(id, true)
    }

    suspend fun completeChallenge(id: Int, email: String, points: Int) = withContext(Dispatchers.IO) {
        ecoTrackDao.updateChallengeCompletionState(id, completed = true)
        ecoTrackDao.addUserPoints(email, points)
    }

    suspend fun awardDirectQuizPoints(email: String, points: Int) = withContext(Dispatchers.IO) {
        ecoTrackDao.addUserPoints(email, points)
    }

    // --- Gemini Recommendation Engine REST API Call ---
    suspend fun getAIRecommendations(logsContext: String): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Needed: Please configure GEMINI_API_KEY in the Secrets panel."
        }

        val prompt = """
            You are "EcoTrack AI", an advanced, friendly sustainability expert and carbon reduction coach.
            Based on the user's current carbon footprint log history, please provide:
            1. An executive summary of their environmental impact.
            2. Concrete, highly actionable recommendations to reduce emissions across Transport, Food, and Energy.
            3. A mini green itinerary (creative step-by-step) to lower their CO2 projection.
            
            Here is the user's activity log detail (category, activity, emission kg CO2):
            $logsContext
            
            Keep your response concise, motivating, and beautifully structured. Use clear headings and simple bullet points. Avoid clinical jargon, and make it engaging.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = geminiApiService.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No suggestions generated. Keep logging to help the AI understand your patterns."
        } catch (e: Exception) {
            "Unable to sync recommendation: ${e.localizedMessage ?: "Network Timeout"}. Please check your connection and try again."
        }
    }
}
