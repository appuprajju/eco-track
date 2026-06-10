package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.CarbonLog
import com.example.data.local.entities.Challenge
import com.example.data.local.entities.Goal
import com.example.data.local.entities.UserStats
import com.example.data.repository.EcoTrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object SignedOut : AuthUiState
    object Loading : AuthUiState
    data class SignedIn(val email: String, val userName: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface ApiResponseState {
    object Idle : ApiResponseState
    object Loading: ApiResponseState
    data class Success(val recommendations: String) : ApiResponseState
    data class Error(val error: String) : ApiResponseState
}

@OptIn(ExperimentalCoroutinesApi::class)
class EcoTrackViewModel(
    private val repository: EcoTrackRepository
) : ViewModel() {

    // --- Authentication State ---
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Key coordinator for multi-user session partitioning
    val currentUserEmail = MutableStateFlow("")

    private var sessionStartTimestamp = 0L

    // For Demo / Verify: Auto ticking countdown timer for session safety
    val sessionTimeRemainingText: StateFlow<String> = flow {
        while (true) {
            val start = sessionStartTimestamp
            if (start == 0L) {
                emit("No active session")
            } else {
                val elapsed = System.currentTimeMillis() - start
                val remaining = (10 * 60 * 60 * 1000L) - elapsed
                if (remaining <= 0) {
                    emit("Session expired! Re-authentication required.")
                } else {
                    val hours = remaining / (1000 * 60 * 60)
                    val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
                    val seconds = (remaining % (1000 * 60)) / 1000
                    emit(String.format("%02dh %02dm %02ds remaining", hours, minutes, seconds))
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10h 00m 00s remaining")

    // --- Gemini API State ---
    private val _geminiState = MutableStateFlow<ApiResponseState>(ApiResponseState.Idle)
    val geminiState: StateFlow<ApiResponseState> = _geminiState.asStateFlow()

    // --- Reactive Partitioned Flows from Room ---
    val allLogs: StateFlow<List<CarbonLog>> = currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getAllCarbonLogs(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<Goal>> = currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getAllGoals(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStats?> = currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(null)
            else repository.getUserStatsFlow(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalEmissions: StateFlow<Double> = currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(0.0)
            else repository.getTotalEmissionsFlow(email).map { it ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Global flows (not tied to specific emails for social challenges)
    val allChallenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedChallengesCount: StateFlow<Int> = repository.completedChallengesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // We prepopulate global default challenges
        viewModelScope.launch {
            repository.insertDefaultChallenges()
        }

        // Active coroutine checking background auto-logout policies every 5 seconds
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                if (_authUiState.value is AuthUiState.SignedIn && sessionStartTimestamp > 0L) {
                    val age = System.currentTimeMillis() - sessionStartTimestamp
                    if (age >= 10 * 60 * 60 * 1000L) { // 10 Hours limit
                        logout("Your session has securely expired after 10 hours. Please sign back in.")
                    }
                }
            }
        }
    }

    // --- Enterprise Authentication & Registration Functions ---
    fun register(email: String, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val trimmedEmail = email.trim()
            val trimmedName = name.trim()

            if (!trimmedEmail.contains("@") || trimmedEmail.length < 5) {
                _authUiState.value = AuthUiState.Error("Invalid email coordinates.")
                onResult(false, "Invalid email format. Must contain '@' and be at least 5 chars.")
                return@launch
            }

            val success = repository.registerUser(trimmedEmail, trimmedName.ifEmpty { "Eco Pioneer" })
            if (success) {
                sessionStartTimestamp = System.currentTimeMillis()
                currentUserEmail.value = trimmedEmail
                _authUiState.value = AuthUiState.SignedIn(trimmedEmail, trimmedName.ifEmpty { "Eco Pioneer" })
                onResult(true, "Register success! Welcome email sent to $trimmedEmail.")
            } else {
                _authUiState.value = AuthUiState.Error("This email is already registered. Please login instead.")
                onResult(false, "This email is already registered. Please login instead.")
            }
        }
    }

    fun login(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val trimmedEmail = email.trim()

            if (!trimmedEmail.contains("@") || trimmedEmail.length < 5) {
                _authUiState.value = AuthUiState.Error("Invalid email coordinates.")
                onResult(false, "Invalid email format. Must contain '@' and be at least 5 chars.")
                return@launch
            }

            val account = repository.getUserAccount(trimmedEmail)
            if (account != null) {
                sessionStartTimestamp = System.currentTimeMillis()
                currentUserEmail.value = trimmedEmail
                repository.getOrCreateUserStats(trimmedEmail, account.name)
                _authUiState.value = AuthUiState.SignedIn(trimmedEmail, account.name)
                onResult(true, "Successfully logged in as ${account.name}!")
            } else {
                _authUiState.value = AuthUiState.Error("This email is not registered yet. Please Register first.")
                onResult(false, "This email is not registered yet. Please Register first.")
            }
        }
    }

    fun logout(message: String = "") {
        viewModelScope.launch {
            _authUiState.value = if (message.isNotEmpty()) AuthUiState.Error(message) else AuthUiState.SignedOut
            currentUserEmail.value = ""
            sessionStartTimestamp = 0L
        }
    }

    // --- Core Logging and Carbon Coefficients ---
    fun logActivity(
        category: String,
        note: String,
        rawValue: Double,
        subOption: String
    ) {
        val email = currentUserEmail.value
        if (email.isEmpty()) return

        if (rawValue <= 0.0 || rawValue > 1_000_000.0) {
            return
        }
        val sanitizedNote = note.trim().take(120).replace(Regex("[<>#%&]"), "")
        val sanitizedSubOption = subOption.trim().take(80).replace(Regex("[<>#%&]"), "")

        viewModelScope.launch {
            val computedCo2 = when (category) {
                "TRANSPORT" -> {
                    when (sanitizedSubOption) {
                        "Diesel Car" -> rawValue * 0.175
                        "Petrol Car" -> rawValue * 0.143
                        "Electric Car" -> rawValue * 0.041
                        "Public Bus/Metro" -> rawValue * 0.052
                        "Flight" -> rawValue * 0.150
                        else -> rawValue * 0.110
                    }
                }
                "ENERGY" -> {
                    when (sanitizedSubOption) {
                        "Coal Grid" -> rawValue * 0.707
                        "Natural Gas" -> rawValue * 0.202
                        "Solar/Wind Grid" -> rawValue * 0.053
                        else -> rawValue * 0.400
                    }
                }
                "FOOD" -> {
                    when (sanitizedSubOption) {
                        "Beef/Pork Heavy" -> rawValue * 6.200
                        "Poultry/Fish" -> rawValue * 2.100
                        "Vegetarian" -> rawValue * 0.540
                        "Vegan Plan" -> rawValue * 0.210
                        else -> rawValue * 1.500
                    }
                }
                "CONSUMPTION" -> {
                    when (sanitizedSubOption) {
                        "Electronics" -> rawValue * 25.0
                        "Apparel/Fast Fashion" -> rawValue * 8.0
                        "Bulk Goods" -> rawValue * 1.5
                        else -> rawValue * 5.0
                    }
                }
                else -> rawValue * 1.0
            }

            repository.logActivity(
                email = email,
                category = category,
                note = "$sanitizedNote ($sanitizedSubOption)",
                rawValue = rawValue,
                unit = when (category) {
                    "TRANSPORT" -> "km"
                    "ENERGY" -> "kWh"
                    "FOOD" -> "meals"
                    "CONSUMPTION" -> "items"
                    else -> "units"
                },
                carbonCo2Kg = computedCo2
            )

            // Dynamic Progress Updates for Active Goals
            updateMatchingGoals(category, computedCo2)
        }
    }

    private suspend fun updateMatchingGoals(category: String, co2SavedOrLogged: Double) {
        val email = currentUserEmail.value
        if (email.isEmpty()) return

        val goalsList = allGoals.value
        for (goal in goalsList) {
            if (!goal.isCompleted && (goal.category == "ALL" || goal.category == category)) {
                val newProgress = goal.currentCo2SavedKg + (co2SavedOrLogged * 0.5)
                val isCompletedNow = newProgress >= goal.targetCo2ReductionKg
                repository.updateGoalProgress(goal.id, email, newProgress.coerceAtMost(goal.targetCo2ReductionKg), isCompletedNow)
            }
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    // --- Goal Management ---
    fun addGoal(title: String, targetReduction: Double, category: String, durationDays: Int) {
        val email = currentUserEmail.value
        if (email.isEmpty()) return

        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)
            val newGoal = Goal(
                userEmail = email,
                title = title,
                targetCo2ReductionKg = targetReduction,
                currentCo2SavedKg = 0.0,
                category = category,
                deadlineTimestamp = deadline,
                isCompleted = false
            )
            repository.addGoal(newGoal)
        }
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    // --- Challenge Trackers ---
    fun joinChallenge(id: Int) {
        viewModelScope.launch {
            repository.joinChallenge(id)
        }
    }

    fun completeChallenge(id: Int, points: Int) {
        val email = currentUserEmail.value
        if (email.isEmpty()) return

        viewModelScope.launch {
            repository.completeChallenge(id, email, points)
        }
    }

    // --- Educational quiz points reward ---
    fun awardQuizBonusPoints(bonus: Int) {
        val email = currentUserEmail.value
        if (email.isEmpty()) return

        viewModelScope.launch {
            repository.awardDirectQuizPoints(email, bonus)
        }
    }

    // --- Simulated Smart Home & IoT Hardware Feed Contexts ---
    private val _simulatedSmartHomeConnected = MutableStateFlow(true)
    val simulatedSmartHomeConnected: StateFlow<Boolean> = _simulatedSmartHomeConnected.asStateFlow()

    private val _simulatedLocationContext = MutableStateFlow("Transit Proximity: <250m to Subway Line B")
    val simulatedLocationContext: StateFlow<String> = _simulatedLocationContext.asStateFlow()

    fun toggleSmartHomeSimulation(connected: Boolean) {
        _simulatedSmartHomeConnected.value = connected
    }

    fun updateSimulatedLocation(context: String) {
        _simulatedLocationContext.value = context
    }

    // --- Trigger Gemini Generative Content Recommendations ---
    fun fetchAIRecommendations() {
        viewModelScope.launch {
            _geminiState.value = ApiResponseState.Loading
            val logs = allLogs.value
            val isSmartHomeOn = _simulatedSmartHomeConnected.value
            val currentLoc = _simulatedLocationContext.value

            val userActivityLogContext = if (logs.isEmpty()) {
                "User has no logs yet. Standard introductory user profile. Focus recommendations on initial simple tips."
            } else {
                logs.take(15).joinToString("\n") { log ->
                    "- Category: ${log.category} | Activity: ${log.note} | Emission: ${String.format("%.2f", log.carbonCo2Kg)} kg CO2"
                }
            }

            val IoTContext = "\n--- Simulated IoT Telemetry Integration ---\n" +
                    "- IoT Smart Home Connected: $isSmartHomeOn (Standby Vampire power optimization enabled: $isSmartHomeOn)\n" +
                    "- User Real-Time Location Context: $currentLoc (Offers localized high-impact public transit alternative shifts)"

            val hybridPayload = userActivityLogContext + IoTContext

            try {
                val recommendation = repository.getAIRecommendations(hybridPayload)
                _geminiState.value = ApiResponseState.Success(recommendation)
            } catch (e: Exception) {
                _geminiState.value = ApiResponseState.Error(e.localizedMessage ?: "Unknown Error occurred")
            }
        }
    }
}

class EcoTrackViewModelFactory(
    private val repository: EcoTrackRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EcoTrackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EcoTrackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
