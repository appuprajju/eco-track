package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekPrimaryDark,
    secondary = SleekSecondaryDark,
    tertiary = SleekTertiaryDark,
    background = SleekBackgroundDark,
    surface = SleekSurfaceDark,
    surfaceVariant = SleekSurfaceVariantDark,
    outline = SleekOutlineDark,
    onPrimary = Color(0xFF103800),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFE2E3DD),
    onBackground = Color(0xFFE2E3DD),
    onSurface = Color(0xFFE2E3DD)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimaryLight,
    secondary = SleekSecondaryLight,
    tertiary = SleekTertiaryLight,
    background = SleekBackgroundLight,
    surface = SleekSurfaceLight,
    surfaceVariant = SleekSurfaceVariantLight,
    outline = SleekOutlineLight,
    onPrimary = Color.White,
    onSecondary = Color(0xFF111F0C),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C18),
    onSurface = Color(0xFF1A1C18)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic system color by default to preserve EcoTrack's green brand identity,
  // making it look perfectly tailored on all Android 12+ devices alike.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
