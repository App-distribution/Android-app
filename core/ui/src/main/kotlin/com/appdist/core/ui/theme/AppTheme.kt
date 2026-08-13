package com.appdist.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    secondary = AppOnSurfaceVariant,
    onSecondary = AppOnPrimary,
    secondaryContainer = AppSurfaceVariant,
    onSecondaryContainer = AppOnSurface,
    background = AppBackground,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = AppDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF174E49),
    onPrimaryContainer = Color(0xFFB8F0E9),
    secondaryContainer = AppDarkSurfaceVariant,
    onSecondaryContainer = AppDarkOnSurface,
    background = AppDarkBackground,
    onBackground = AppDarkOnSurface,
    surface = AppDarkSurface,
    onSurface = AppDarkOnSurface,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = AppDarkOnSurfaceVariant,
    outline = AppDarkOutline,
    outlineVariant = Color(0xFF2B3D41),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
