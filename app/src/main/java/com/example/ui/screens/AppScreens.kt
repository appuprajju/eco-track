package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entities.CarbonLog
import com.example.data.local.entities.Challenge
import com.example.data.local.entities.Goal
import com.example.ui.accessibility.AccessibilityModifiers.wcagHeading
import com.example.ui.accessibility.AccessibilityModifiers.wcagClickable
import com.example.ui.accessibility.AccessibilityModifiers.wcagLiveRegion
import com.example.ui.accessibility.AccessibilityModifiers.wcagInformationBlock
import com.example.ui.accessibility.AccessibilityModifiers.wcagToggleSemantics
import com.example.ui.viewmodel.ApiResponseState
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.EcoTrackViewModel
import kotlin.math.roundToInt

// Navigation Routes
const val ROUTE_SPLASH = "splash"
const val ROUTE_AUTH = "auth"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CALCULATOR = "calculator"
const val ROUTE_AI_COACH = "ai_coach"
const val ROUTE_GOALS = "goals_challenges"
const val ROUTE_LEARNING = "learning_hub"
const val ROUTE_PROFILE = "profile"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoTrackMainApp(viewModel: EcoTrackViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_AUTH

    Scaffold(
        bottomBar = {
            if (authState is AuthUiState.SignedIn) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val tabs = listOf(
                        NavigationBarItemData(ROUTE_DASHBOARD, Icons.Default.Home, "Overview"),
                        NavigationBarItemData(ROUTE_CALCULATOR, Icons.Default.AddCircle, "Calculator"),
                        NavigationBarItemData(ROUTE_AI_COACH, Icons.Default.Psychology, "Eco Coach"),
                        NavigationBarItemData(ROUTE_GOALS, Icons.Default.EmojiEvents, "Action"),
                        NavigationBarItemData(ROUTE_LEARNING, Icons.Default.School, "Learn")
                    )

                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_SPLASH) {
                SplashScreen(navController = navController, viewModel = viewModel)
            }
            composable(ROUTE_AUTH) {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate(ROUTE_DASHBOARD) {
                            popUpTo(ROUTE_AUTH) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_DASHBOARD) {
                DashboardScreen(viewModel = viewModel, navController = navController)
            }
            composable(ROUTE_CALCULATOR) {
                CalculatorScreen(viewModel = viewModel)
            }
            composable(ROUTE_AI_COACH) {
                AiCoachScreen(viewModel = viewModel)
            }
            composable(ROUTE_GOALS) {
                GoalsAndChallengesScreen(viewModel = viewModel)
            }
            composable(ROUTE_LEARNING) {
                LearningHubScreen(viewModel = viewModel)
            }
            composable(ROUTE_PROFILE) {
                ProfileScreen(viewModel = viewModel, navController = navController)
            }
        }
    }
}

data class NavigationBarItemData(
    val route: String,
    val icon: ImageVector,
    val label: String
)

// ========================
// 1. AUTH SCREEN MODULE
// ========================
@Composable
fun AuthScreen(viewModel: EcoTrackViewModel, onAuthSuccess: () -> Unit) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Register, 1 = Login
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthUiState.SignedIn) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EcoTrack App Logo Mark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "EcoTrack AI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Personal Carbon Footprint Intelligence Platform",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Tab Row for Register vs Login
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Register", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Login", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Input Icon") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Input Icon") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                if (authState is AuthUiState.Error) {
                    Text(
                        text = (authState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.register(email, name) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            viewModel.login(email) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (authState is AuthUiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (selectedTab == 0) "Register Account" else "Secure Login", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Text(
                    text = "🔐 All sessions are active for 10 hours and protected with local privacy isolation.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================
// 2. DASHBOARD SCREEN
// ==========================
@Composable
fun DashboardScreen(viewModel: EcoTrackViewModel, navController: NavController) {
    val totalCo2 by viewModel.totalEmissions.collectAsStateWithLifecycle()
    val stats by viewModel.userStats.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val completedChallengesCount by viewModel.completedChallengesCount.collectAsStateWithLifecycle()

    // Calculate Category footprint stats for Canvas Charts
    val logStatsMap = remember(logs) {
        val mapped = mutableMapOf("TRANSPORT" to 0.0, "ENERGY" to 0.0, "FOOD" to 0.0, "CONSUMPTION" to 0.0)
        logs.forEach { log ->
            mapped[log.category] = (mapped[log.category] ?: 0.0) + log.carbonCo2Kg
        }
        mapped
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App bar panel (Sleek Interface style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = "Eco symbol",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "EcoTrack AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "PERSONAL INTELLIGENCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(ROUTE_PROFILE) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hello Greeting & Level badge
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stats?.userName ?: "Pioneer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Level ${stats?.level ?: 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stats?.level ?: 1}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }

        // Gamification metrics row (Sleek Outline style)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Points Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Carbon Points", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${stats?.points ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Streaks Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFF5722))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active Streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${stats?.consecutiveLoginStreak ?: 0} Days", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Badges Card (Completed Challenges)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Actions Met", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("$completedChallengesCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Activity Quick Tracking (Sleek Layout Pattern)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Tracking",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .wcagHeading()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickItems = listOf(
                        Triple("Transport", Icons.Default.DirectionsCar, "TRANSPORT"),
                        Triple("Diet", Icons.Default.Restaurant, "FOOD"),
                        Triple("Energy", Icons.Default.Bolt, "ENERGY")
                    )
                    quickItems.forEach { (label, icon, cat) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .wcagClickable(
                                    label = "Quick track $label activity",
                                    onClickLabel = "Log and compute carbon emission for $label"
                                ) {
                                    navController.navigate(ROUTE_CALCULATOR)
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Carbon circular trend and summary (Sleek Rich Contrast Card Style)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wcagHeading()
                    .wcagLiveRegion(
                        descriptionText = "Total Carbon Footprint summary",
                        currentValue = String.format("%.2f kilograms of CO2 equivalent", totalCo2)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Carbon Footprint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.2f", totalCo2),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kg CO₂e",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (totalCo2 == 0.0) "Your footprint is completely neutral! Log daily logs below."
                            else if (totalCo2 < 50.0) "Excellent! Keep remaining green with dynamic commuting and plant-diet choices."
                            else "Your carbon footprint exceeds safe targeted limits. Consult the Eco Coach.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }

                    // Adaptive interactive Canvas Pie Chart
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .weight(0.8f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            val transportVal = logStatsMap["TRANSPORT"] ?: 0.0
                            val energyVal = logStatsMap["ENERGY"] ?: 0.0
                            val foodVal = logStatsMap["FOOD"] ?: 0.0
                            val consumVal = logStatsMap["CONSUMPTION"] ?: 0.0
                            val sum = transportVal + energyVal + foodVal + consumVal

                            if (sum == 0.0) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.25f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 16f)
                                )
                            } else {
                                val transportSweep = ((transportVal / sum) * 360f).toFloat()
                                val energySweep = ((energyVal / sum) * 360f).toFloat()
                                val foodSweep = ((foodVal / sum) * 360f).toFloat()
                                val consumSweep = ((consumVal / sum) * 360f).toFloat()

                                var startAngle = -90f
                                // Transport: Red Coral Accent
                                drawArc(
                                    color = Color(0xFFFF8A80),
                                    startAngle = startAngle,
                                    sweepAngle = transportSweep,
                                    useCenter = false,
                                    size = size,
                                    style = Stroke(16f, cap = StrokeCap.Round)
                                )
                                startAngle += transportSweep

                                // Energy: Gold Accent
                                drawArc(
                                    color = Color(0xFFFFD54F),
                                    startAngle = startAngle,
                                    sweepAngle = energySweep,
                                    useCenter = false,
                                    size = size,
                                    style = Stroke(16f, cap = StrokeCap.Round)
                                )
                                startAngle += energySweep

                                // Food: Mint Green Accent
                                drawArc(
                                    color = Color(0xFF81C784),
                                    startAngle = startAngle,
                                    sweepAngle = foodSweep,
                                    useCenter = false,
                                    size = size,
                                    style = Stroke(16f, cap = StrokeCap.Round)
                                )
                                startAngle += foodSweep

                                // Consumption: Purple Accent
                                drawArc(
                                    color = Color(0xFFB39DDB),
                                    startAngle = startAngle,
                                    sweepAngle = consumSweep,
                                    useCenter = false,
                                    size = size,
                                    style = Stroke(16f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Carbon Category indicators
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryLabel(name = "Commute", value = logStatsMap["TRANSPORT"] ?: 0.0, color = Color(0xFFEF5350))
                CategoryLabel(name = "Utility", value = logStatsMap["ENERGY"] ?: 0.0, color = Color(0xFFFFB300))
                CategoryLabel(name = "Food", value = logStatsMap["FOOD"] ?: 0.0, color = Color(0xFF34D399))
                CategoryLabel(name = "Goods", value = logStatsMap["CONSUMPTION"] ?: 0.0, color = Color(0xFF818CF8))
            }
        }

        // Recent emissions logs header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emissions Activity Log",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = { navController.navigate(ROUTE_CALCULATOR) }
                ) {
                    Text("Audit Log")
                }
            }
        }

        // Sub logs list
        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No recorded log events. Go to the Calculator to log transport, utility, or dietary carbon profiles.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(logs.take(5)) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("carbon_log_item_${log.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Category Icon
                            val iconPair = when (log.category) {
                                "TRANSPORT" -> Pair(Icons.Default.DirectionsCar, Color(0xFFEF5350))
                                "ENERGY" -> Pair(Icons.Default.Bolt, Color(0xFFFFB300))
                                "FOOD" -> Pair(Icons.Default.Restaurant, Color(0xFF34D399))
                                else -> Pair(Icons.Default.ShoppingCart, Color(0xFF818CF8))
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(iconPair.second.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconPair.first, contentDescription = null, tint = iconPair.second, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(log.note, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = "Quantity: ${log.rawValue} ${log.unit}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.2f kg", log.carbonCo2Kg),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("CO₂e", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.deleteLog(log.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete log",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryLabel(name: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(String.format("%.1f kg", value), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ======================================
// 3. CARBON CALCULATOR & LOGGER SCREEN
// ======================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: EcoTrackViewModel) {
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var category by remember { mutableStateOf("TRANSPORT") } // TRANSPORT, ENERGY, FOOD, CONSUMPTION
    var rawInput by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Sub-option selection
    val optionsMap = mapOf(
        "TRANSPORT" to listOf("Diesel Car", "Petrol Car", "Electric Car", "Public Bus/Metro", "Flight"),
        "ENERGY" to listOf("Coal Grid", "Natural Gas", "Solar/Wind Grid"),
        "FOOD" to listOf("Beef/Pork Heavy", "Poultry/Fish", "Vegetarian", "Vegan Plan"),
        "CONSUMPTION" to listOf("Electronics", "Apparel/Fast Fashion", "Bulk Goods")
    )

    var subOption by remember(category) { mutableStateOf(optionsMap[category]!![0]) }

    // Live emission preview math
    val liveEmissions = remember(category, rawInput, subOption) {
        val raw = rawInput.toDoubleOrNull() ?: 0.0
        when (category) {
            "TRANSPORT" -> {
                when (subOption) {
                    "Diesel Car" -> raw * 0.175
                    "Petrol Car" -> raw * 0.143
                    "Electric Car" -> raw * 0.041
                    "Public Bus/Metro" -> raw * 0.052
                    "Flight" -> raw * 0.150
                    else -> raw * 0.110
                }
            }
            "ENERGY" -> {
                when (subOption) {
                    "Coal Grid" -> raw * 0.707
                    "Natural Gas" -> raw * 0.202
                    "Solar/Wind Grid" -> raw * 0.053
                    else -> raw * 0.400
                }
            }
            "FOOD" -> {
                when (subOption) {
                    "Beef/Pork Heavy" -> raw * 6.200
                    "Poultry/Fish" -> raw * 2.100
                    "Vegetarian" -> raw * 0.540
                    "Vegan Plan" -> raw * 0.210
                    else -> raw * 1.500
                }
            }
            "CONSUMPTION" -> {
                when (subOption) {
                    "Electronics" -> raw * 25.0
                    "Apparel/Fast Fashion" -> raw * 8.0
                    "Bulk Goods" -> raw * 1.5
                    else -> raw * 5.0
                }
            }
            else -> raw * 1.0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Carbon Footprint Calculator", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Estimate emissions based on verified international conversion factors.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        // Carbon Category selectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    Triple("TRANSPORT", Icons.Default.DirectionsCar, "Transit"),
                    Triple("ENERGY", Icons.Default.Bolt, "Utility"),
                    Triple("FOOD", Icons.Default.Restaurant, "Food"),
                    Triple("CONSUMPTION", Icons.Default.ShoppingCart, "Goods")
                )

                cats.forEach { (catId, icon, label) ->
                    val selected = category == catId
                    Button(
                        onClick = { category = catId },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Input Fields Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Log Entry Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // Sub category selection row
                    Text("Select Specific Sub-type:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        optionsMap[category]!!.forEach { opt ->
                            val sSelected = subOption == opt
                            FilterChip(
                                selected = sSelected,
                                onClick = { subOption = opt },
                                label = { Text(opt, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Activity Description (e.g. Commute to Office)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = it },
                        label = {
                            Text(
                                "Quantity (" + when (category) {
                                    "TRANSPORT" -> "kilometers"
                                    "ENERGY" -> "kWh energy"
                                    "FOOD" -> "meals consumed"
                                    else -> "commodities logged"
                                } + ")"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carbon_quantity_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live Carbon Preview Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CO₂e Core Impact Projection:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = String.format("%.3f kg", liveEmissions),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val raw = rawInput.toDoubleOrNull()
                            if (raw == null || raw <= 0) {
                                Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.logActivity(
                                category = category,
                                note = note.ifEmpty { "Eco Log" },
                                rawValue = raw,
                                subOption = subOption
                            )
                            Toast.makeText(context, "Carbon Footprint Logged!", Toast.LENGTH_SHORT).show()
                            // reset
                            rawInput = ""
                            note = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("log_carbon_submit"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Record to Offline Ledger", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Ledger Overview title
        item {
            Text("Ledder Ledger - Recorded Logs", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Full Interactive list
        if (logs.isEmpty()) {
            item {
                Text(
                    "Your audit logs are currently completely empty. Estimate emissions above to build your secure record.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.note, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Category: ${log.category} | ${log.rawValue} ${log.unit}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format("%.2f kg", log.carbonCo2Kg), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteLog(log.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================
// 4. AI COACH SCREEN (GEMINI)
// ==========================
@Composable
fun AiCoachScreen(viewModel: EcoTrackViewModel) {
    val geminiState by viewModel.geminiState.collectAsStateWithLifecycle()
    val simulatedSmartHomeConnected by viewModel.simulatedSmartHomeConnected.collectAsStateWithLifecycle()
    val simulatedLocationContext by viewModel.simulatedLocationContext.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Eco Coach AI Recommendation Engine", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Powered by Gemini models client-side evaluating real carbon footprint statistics.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Green Recommendation Diagnostic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Text(
                        text = "Your Eco Coach will securely analyze your logged carbon output metrics across Transportation, Utility energy, Food diet, and Purchase commodities to synthesize a personalized green transition itinerary.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.fetchAIRecommendations() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer, contentColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_coach_spark_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Synthesize Green Roadmap", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Interactive External APIs Telemetry Simulator (Automation & Accuracy Demonstration)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = "Simulated Energy Feed Icon", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulated Smart Home Integration", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        androidx.compose.material3.Switch(
                            checked = simulatedSmartHomeConnected,
                            onCheckedChange = { viewModel.toggleSmartHomeSimulation(it) },
                            modifier = Modifier.wcagToggleSemantics(
                                label = "Simulated Nest and SmartThings Smart Home Integration telemetry",
                                isToggled = simulatedSmartHomeConnected,
                                onToggleLabel = "Toggle background power optimization parameters"
                            )
                        )
                    }
                    Text(
                        text = "Enable Smart Home IoT telemetry. When enabled, Nest/SmartThings parameters (like background vampire draw) are calculated and fed into recommendations.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    OutlinedTextField(
                        value = simulatedLocationContext,
                        onValueChange = { viewModel.updateSimulatedLocation(it) },
                        label = { Text("Simulated Location (e.g. coordinates or transit hubs)") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        singleLine = true
                    )
                }
            }
        }

        // Gemini Diagnostic outputs
        item {
            when (val state = geminiState) {
                is ApiResponseState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "💡 Click the roadmap button to trigger Gemini diagnostic recommendations.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                is ApiResponseState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Analyzing statistics on client secure neural sandbox...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                is ApiResponseState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Green Coach Advisory:", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.recommendations,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                is ApiResponseState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Diagnostic Failed: " + state.error,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================================
// 5. GOALS & CHALLENGES SCREEN MODULE
// ======================================
@Composable
fun GoalsAndChallengesScreen(viewModel: EcoTrackViewModel) {
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val challenges by viewModel.allChallenges.collectAsStateWithLifecycle()

    var showAddGoalDialog by remember { mutableStateOf(false) }

    var goalTitle by remember { mutableStateOf("") }
    var targetReduction by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TRANSPORT") }
    var durationDays by remember { mutableStateOf("30") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Carbon Target Goals", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Manage offsets & carbon savings milestones.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }

                Button(
                    onClick = { showAddGoalDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    Text("New Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List Active Goals
        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No active carbon goals set. Set a goal above!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            items(goals) { goal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(goal.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Category: ${goal.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (goal.isCompleted) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    IconButton(onClick = { viewModel.deleteGoal(goal.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress bar calculation
                        val progressPct = remember(goal) {
                            if (goal.targetCo2ReductionKg == 0.0) 0f
                            else (goal.currentCo2SavedKg / goal.targetCo2ReductionKg).toFloat().coerceIn(0f, 1f)
                        }

                        LinearProgressIndicator(
                            progress = progressPct,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Saved: ${String.format("%.1f", goal.currentCo2SavedKg)} kg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Target Reduction: ${String.format("%.1f", goal.targetCo2ReductionKg)} kg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Community challenges title
        item {
            Text("Community Action Challenges", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        items(challenges) { challenge ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (challenge.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Premium icon like workspace_premium
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Active action Premium icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = challenge.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = challenge.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+${challenge.pointsAwarded} Pts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Bottom Row: styled with a container background exactly like HTML bg-[#f0f3e8] p-3 rounded-xl
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${challenge.participantsCount} active citizens",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (challenge.isCompleted) {
                            ElevatedButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.elevatedButtonColors(disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text("Fulfilled", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (challenge.isJoined) {
                            Button(
                                onClick = { viewModel.completeChallenge(challenge.id, challenge.pointsAwarded) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("I Did This!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.joinChallenge(challenge.id) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Join Action", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Define Carbon Saving Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g., Transit offset)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetReduction,
                        onValueChange = { targetReduction = it },
                        label = { Text("Target CO₂ Reduction (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Text("Category Linkage:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val cats = listOf("ALL", "TRANSPORT", "ENERGY", "FOOD", "CONSUMPTION")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cats.forEach { c ->
                            val selected = selectedCategory == c
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("Duration Days (e.g. 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reductionVal = targetReduction.toDoubleOrNull()
                        val days = durationDays.toIntOrNull()
                        if (goalTitle.isNotEmpty() && reductionVal != null && reductionVal > 0 && days != null) {
                            viewModel.addGoal(goalTitle, reductionVal, selectedCategory, days)
                            showAddGoalDialog = false
                            // Reset
                            goalTitle = ""
                            targetReduction = ""
                            selectedCategory = "TRANSPORT"
                            durationDays = "30"
                        }
                    }
                ) {
                    Text("Add Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================
// 6. SUSTAIN LEARNING HUB SCREEN
// ==========================
@Composable
fun LearningHubScreen(viewModel: EcoTrackViewModel) {
    var activeArticleIndex by remember { mutableStateOf(0) }
    var selectedQuizOption by remember { mutableStateOf<Int?>(null) }
    var quizMessage by remember { mutableStateOf("") }
    var pointsEarnedBonus by remember { mutableIntStateOf(0) }

    val articles = listOf(
        LearningArticle(
            title = "What is the Nitrogen Carbon Balance?",
            content = "Excess carbon dioxide and synthetic nitrogen runoffs are disrupting marine biology and agricultural health globally. Agricultural fertilizer emissions, largely fueled by dense beef heavy manufacturing diets, release massive nitrous oxide which is 300x more potent than standard CO2 molecules. Transitioning even 1 meal per day to a local organic vegan plant diet saves over 5kg in equivalent direct nitrous greenhouse offsets.",
            question = "Nitrous oxide represents how many times more warming potential than normal CO2?",
            options = listOf("10 times", "50 times", "300 times", "1000 times"),
            correctOptionIndex = 2
        ),
        LearningArticle(
            title = "Decarbonizing Vampire Energy Draw",
            content = "Even when appliances are completely switched off, standby vampire currents keep draining massive residual wattages from electric sockets. Up to 10% of standard household utility carbon bills trace directly to plugged-in laptops, internet routers, phone chargers and modern smart television consoles. Simply utilizing smart power strips or unplugging devices fully before sleep can reduce household grid energy carbon release by as much as 100kg of CO2 equivalent annually.",
            question = "Standby vampire current draw accounts for roughly what percent of home energy draft?",
            options = listOf("Under 1%", "Approximately 10%", "Exactly 45%", "Over 80%"),
            correctOptionIndex = 1
        ),
        LearningArticle(
            title = "Air Transportation Contrail Physics",
            content = "High-altitude flight contrails lock condensation vapor ice caps in the stratosphere, amplifying net radiative trapping forces. Jet fuel releases immediate CO2 and heavy carbon black particulates. Short regional flights are much more carbon intensive per passenger than long international lines owing to the enormous thrust energy consumed exclusively during takeoff. Walking or utilizing high-speed rail lines instead saves immense atmospheric loading.",
            question = "Which leg of a commercial passenger flight uses the highest density of fuel and energy?",
            options = "Cruising phase,Takeoff thrust phase,Landing descent,Security taxiing".split(","),
            correctOptionIndex = 1
        )
    )

    val currentArticle = articles[activeArticleIndex]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Sustainability Learning Hub", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Complete quizzes to test knowledge and earn bonus carbon points.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        // Horizontal toggle row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                articles.forEachIndexed { index, art ->
                    val selected = index == activeArticleIndex
                    ElevatedFilterChip(
                        selected = selected,
                        onClick = {
                            activeArticleIndex = index
                            selectedQuizOption = null
                            quizMessage = ""
                        },
                        label = { Text("Topic ${index + 1}: ${art.title.take(15)}...", fontSize = 12.sp) }
                    )
                }
            }
        }

        // Active Article Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        currentArticle.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        currentArticle.content,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Interactive Topic Quiz
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Interactive Quiz - Earn Goal points!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(currentArticle.question, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)

                    currentArticle.options.forEachIndexed { optIndex, optText ->
                        val selected = selectedQuizOption == optIndex
                        val btnColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        val txtColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                        OutlinedButton(
                            onClick = {
                                selectedQuizOption = optIndex
                                if (optIndex == currentArticle.correctOptionIndex) {
                                    quizMessage = "🎉 CORRECT! Nitrous oxide and vampire wattages have massive carbon impact. Earned 50 Bonus points!"
                                    viewModel.awardQuizBonusPoints(50)
                                } else {
                                    quizMessage = "❌ Incorrect option. Read topic details above and try again!"
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = btnColor, contentColor = txtColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(optText, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    if (quizMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = quizMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedQuizOption == currentArticle.correctOptionIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LearningArticle(
    val title: String,
    val content: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

// ==========================
// 8. PROFILE SCREEN MODULE
// ==========================
@Composable
fun ProfileScreen(viewModel: EcoTrackViewModel, navController: NavController) {
    val stats by viewModel.userStats.collectAsStateWithLifecycle()
    val email by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val sessionTimeRemaining by viewModel.sessionTimeRemainingText.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top row navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Profile & Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Avatar / Leaf Graphic
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
        }

        Text(
            text = stats?.userName ?: "Eco Pioneer",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Account Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Registered Email", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(email, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Points status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Carbon Points", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${stats?.points ?: 0} pts", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Level status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Eco Level achieved", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Level ${stats?.level ?: 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Streak status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Consecutive Login Streak", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("${stats?.consecutiveLoginStreak ?: 1} Days 🔥", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Session Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "This session automatically expires after 10 hours for database protection.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = sessionTimeRemaining,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Force Logout Button
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("force_logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Force Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}
