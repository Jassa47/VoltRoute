package com.example.voltroute.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color.White,
    primaryContainer = PrimaryCyanDark,
    onPrimaryContainer = Color.White,

    secondary = SecondaryGold,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryGoldDark,
    onSecondaryContainer = Color.White,

    tertiary = SecondaryGold,
    onTertiary = Color.Black,

    background = BackgroundDark,
    onBackground = Color.White,

    surface = SurfaceDark,
    onSurface = Color.White,

    surfaceVariant = SurfaceDark,
    onSurfaceVariant = Color(0xFFB0BEC5),

    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3E5FC),
    onPrimaryContainer = Color.Black,

    secondary = SecondaryGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color.Black,

    tertiary = SecondaryGoldDark,
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = Color.Black,

    surface = SurfaceLight,
    onSurface = Color.Black,

    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),

    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun VoltRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}