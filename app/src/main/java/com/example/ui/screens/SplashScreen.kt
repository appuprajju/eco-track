package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.R
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.EcoTrackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: EcoTrackViewModel
) {
    // Collect AuthUiState to know where to route when animation finishes
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()

    // Animatable states
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(-20f) }
    
    // Ripple stroke expansion
    val rippleAnimation = rememberInfiniteTransition(label = "pulse_ripple")
    val rippleScale by rippleAnimation.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    val rippleAlpha by rippleAnimation.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    // Fade-in slides for text
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }

    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Run entry animations in parallel
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        // Staggered text fade in
        delay(600)
        launch {
            textAlpha.animateTo(1f, tween(800, easing = EaseOutQuad))
        }
        launch {
            textOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
        }
        
        // Subtitle delay
        delay(300)
        subtitleAlpha.animateTo(1f, tween(1000, easing = EaseOutQuad))

        // Total splash duration before redirecting
        delay(1400)

        // Branch redirect safely
        val targetRoute = if (authState is AuthUiState.SignedIn) {
            ROUTE_DASHBOARD
        } else {
            ROUTE_AUTH
        }

        navController.navigate(targetRoute) {
            popUpTo(ROUTE_SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF11140E), // Match BackgroundDark
                        Color(0xFF1F291B), // Soft organic deep sage
                        Color(0xFF0F120D)  
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ripple layer background for the centered brand logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Expanding active analytics circles (Pulse Wave)
                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(rippleScale)
                        .alpha(rippleAlpha)
                ) {
                    drawCircle(
                        color = Color(0xFF9CD67D),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Inner Glassmorphic-ish Brand Solid Accent Disc
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                        .graphicsLayer(rotationZ = rotation.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF23281E),
                                    Color(0xFF171B14)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "EcoTrack AI Modern Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Typography Block
            Text(
                text = "EcoTrack AI",
                color = Color(0xFFFFFFFF),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Personal Carbon Intelligence",
                color = Color(0xFF9CD67D), // Primary contrast theme shade
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
        
        // Version tracker or bottom details
        Text(
            text = "Telemetry & AI Edition • v1.1",
            color = Color(0xFF8B9284).copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(subtitleAlpha.value)
        )
    }
}
