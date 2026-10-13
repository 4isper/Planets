package com.m4isper.planets.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NasaBlue,
    onPrimary = Starlight,
    secondary = MistBlue,
    onSecondary = SpaceBlack,
    tertiary = NasaRed,
    background = SpaceBlack,
    onBackground = Starlight,
    surface = SpaceNavy,
    onSurface = Starlight,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = MistBlue,
)

private val LightColorScheme = lightColorScheme(
    primary = NasaBlueDark,
    onPrimary = Starlight,
    secondary = NasaBlueDark,
    onSecondary = Starlight,
    tertiary = NasaRed,
    onTertiary = Starlight,
    background = Starlight,
    onBackground = Starcheck,
    surface = Starlight,
    onSurface = Starcheck,
    surfaceVariant = SpaceSurface,
    onSurfaceVariant = SpaceNavy,
)

@Composable
fun PlanetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
        content = content,
    )
}