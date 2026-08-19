package com.appdist.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Схема Material выводится из фирменных токенов: тогда компоненты M3 без
 * явно заданного цвета попадают в тему, а не в дефолтную палитру Material.
 */
internal fun AppColors.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = ground,
        primaryContainer = action,
        onPrimaryContainer = ink,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = ink,
        background = ground,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outline,
        // AlertDialog берёт контейнер из surfaceContainerHigh — без этой
        // привязки к токенам диалог рисуется на нейтральной поверхности
        // Material с фиолетовым оттенком, которой нет в палитре нигде.
        surfaceContainerLowest = ground,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceTint = accent,
    )
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) volcanicDarkColors() else volcanicLightColors()

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = AppTypography,
            content = content
        )
    }
}
