package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    background = SleekDarkBackground,
    onBackground = SleekDarkOnBackground,
    surface = SleekDarkSurface,
    onSurface = SleekDarkOnSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant,
    outline = SleekDarkOnSurfaceVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekBlue,
    onPrimary = SleekSurface,
    primaryContainer = SleekLightBlue,
    onPrimaryContainer = SleekNavy,
    background = SleekBackground,
    onBackground = SleekTextMain,
    surface = SleekSurface,
    onSurface = SleekTextMain,
    surfaceVariant = SleekBorderLight,
    onSurfaceVariant = SleekTextSub,
    outline = SleekOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (disabled by default to honor Jordan's Sleek Interface branding)
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
