package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppColorsTest {

    @Test
    fun `тёмная палитра совпадает с Clean Ledger`() {
        val c = volcanicDarkColors()
        assertEquals(Color(0xFF17161A), c.ground)
        assertEquals(Color(0xFF232127), c.surface)
        assertEquals(Color(0xFFF3F0F2), c.ink)
        assertEquals(Color(0xFFB2ABB3), c.muted)
        assertEquals(Color(0xFFC9C7DB), c.accent)
        assertEquals(Color(0xFF6F6A7C), c.progress)
        assertEquals(Color(0xFFF3F0F2), c.actionTile)
        assertEquals(Color(0xFF232127), c.onActionTile)
    }

    @Test
    fun `светлая палитра совпадает с Clean Ledger`() {
        val c = volcanicLightColors()
        assertEquals(Color(0xFFFCFBF8), c.ground)
        assertEquals(Color.White, c.surface)
        assertEquals(Color(0xFF2B2A28), c.ink)
        assertEquals(Color(0xFF65635F), c.muted)
        assertEquals(Color(0xFFD8D5CF), c.panelStroke)
        assertEquals(Color(0xFFC9C7DB), c.progress)
        assertEquals(Color(0xFF2B2A28), c.actionTile)
        assertEquals(Color.White, c.onActionTile)
    }

    @Test
    fun `темы различаются по фону и основному действию`() {
        val dark = volcanicDarkColors()
        val light = volcanicLightColors()
        assert(light.ground != dark.ground)
        assert(light.actionTile != dark.actionTile)
    }

    @Test
    fun `Material ColorScheme использует семантические токены`() {
        val c = volcanicDarkColors()
        val scheme = c.toColorScheme(darkTheme = true)

        assertEquals(c.actionTile, scheme.primary)
        assertEquals(c.onActionTile, scheme.onPrimary)
        assertEquals(c.accentBright, scheme.primaryContainer)
        assertEquals(c.ink, scheme.onPrimaryContainer)
        assertEquals(c.ground, scheme.background)
        assertEquals(c.ink, scheme.onBackground)
        assertEquals(c.surface, scheme.surface)
        assertEquals(c.ink, scheme.onSurface)
        assertEquals(c.surfaceRaised, scheme.surfaceVariant)
        assertEquals(c.muted, scheme.onSurfaceVariant)
        assertEquals(c.outline, scheme.outline)
    }
}
