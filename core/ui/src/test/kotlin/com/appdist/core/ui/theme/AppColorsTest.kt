package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Снимок тёмной палитры. Значения перенесены из Color.kt до миграции на токены:
 * тест падает, если тёмная тема поехала при правках светлой.
 */
class AppColorsTest {

    @Test
    fun `тёмная палитра совпадает со снимком`() {
        val c = volcanicDarkColors()
        assertEquals(Color(0xFF080B0C), c.ground)
        assertEquals(Color(0xFF111718), c.surface)
        assertEquals(Color(0xFF182123), c.surfaceRaised)
        assertEquals(Color(0xCC10191A), c.panel)
        assertEquals(Color(0x55FFFFFF), c.panelStroke)
        assertEquals(Color(0x66FFFFFF), c.panelStrokeStrong)
        assertEquals(Color(0x99050808), c.field)
        assertEquals(Color(0xAA050808), c.chip)
        assertEquals(Color(0xFF263637), c.track)
        assertEquals(Color(0xFF30433F), c.outline)
        assertEquals(Color(0xFFECF1EF), c.ink)
        assertEquals(Color(0xFFD5E0DD), c.inkSoft)
        assertEquals(Color(0xFFA9BCB8), c.muted)
        assertEquals(Color(0xFF8FA7A3), c.mutedDim)
        assertEquals(Color(0x08FFFFFF), c.ghost)
        assertEquals(Color(0x33AFC5C0), c.ghostLabel)
        assertEquals(Color(0xFF9DDDD2), c.accent)
        assertEquals(Color(0xFFC9FFF5), c.accentBright)
        assertEquals(Color(0xFF063F47), c.action)
        assertEquals(Color(0xFFECF1EF), c.actionTile)
        assertEquals(Color(0xFF063F47), c.onActionTile)
        assertEquals(Color(0xFF052F35), c.railTarget)
        assertEquals(Color(0x33FFFFFF), c.railTargetStroke)
        assertEquals(Color(0x66FFFFFF), c.railDivider)
        assertEquals(Color(0x387FA09A), c.graph)
        assertEquals(Color(0xFF376C67), c.nodeRelated)
        assertEquals(Color(0x777FA09A), c.nodeOutline)
        assertEquals(Color(0x99050808), c.previewGround)
        assertEquals(Color(0xFF0C6974), c.actionPreview)
        assertEquals(Color(0xFFFFB4AB), c.statusMandatory)
        assertEquals(Color(0xFF4A1D1B), c.statusMandatoryContainer)
        assertEquals(Color(0xFFCAD4D1), c.statusDeprecated)
        assertEquals(Color(0xFF29302F), c.statusDeprecatedContainer)
        assertEquals(Color(0xFFFFD18A), c.statusExpiring)
        assertEquals(Color(0xFF4A3210), c.statusExpiringContainer)
        assertEquals(Color(0xFF9DDDD2), c.statusLatest)
        assertEquals(Color(0xFF063F47), c.statusLatestContainer)
    }

    @Test
    fun `светлая палитра отличается от тёмной по фону и тексту`() {
        val dark = volcanicDarkColors()
        val light = volcanicLightColors()
        assertEquals(Color(0xFFF8FAF7), light.ground)
        assertEquals(Color(0xFF132D29), light.ink)
        assertEquals(Color(0xFF0B756C), light.accent)
        assert(light.ground != dark.ground)
        assert(light.ink != dark.ink)
    }
}
