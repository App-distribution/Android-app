package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Контраст по WCAG 2.1. Порог 4.5:1 — текстовые пары (AA). Полупрозрачные
 * токены сначала накладываются на свой фон. Отдельно — sanity-check, что
 * декоративные обводки (panelStroke, track) не сливаются с фоном; это не
 * WCAG-требование, см. комментарий у теста.
 */
class LightPaletteContrastTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background).luminance().toDouble()
        val bg = background.luminance().toDouble()
        val lighter = maxOf(fg, bg)
        val darker = minOf(fg, bg)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun `текст светлой темы проходит AA`() {
        val c = volcanicLightColors()
        val panelOverGround = c.panel.compositeOver(c.ground)
        assertTrue(contrast(c.ink, c.ground) >= 4.5) { "ink на ground" }
        assertTrue(contrast(c.ink, panelOverGround) >= 4.5) { "ink на panel" }
        assertTrue(contrast(c.inkSoft, panelOverGround) >= 4.5) { "inkSoft на panel" }
        assertTrue(contrast(c.muted, c.ground) >= 4.5) { "muted на ground" }
        assertTrue(contrast(c.mutedDim, c.ground) >= 4.5) { "mutedDim на ground" }
        assertTrue(contrast(c.accent, c.ground) >= 4.5) { "accent на ground" }
        assertTrue(contrast(c.onActionTile, c.actionTile) >= 4.5) { "текст на CTA-плитке" }
        assertTrue(contrast(c.ink, c.action) >= 4.5) { "ink на teal-рельсе" }
    }

    @Test
    fun `статусные чипы светлой темы проходят AA`() {
        val c = volcanicLightColors()
        assertTrue(contrast(c.statusMandatory, c.statusMandatoryContainer) >= 4.5)
        assertTrue(contrast(c.statusDeprecated, c.statusDeprecatedContainer) >= 4.5)
        assertTrue(contrast(c.statusExpiring, c.statusExpiringContainer) >= 4.5)
        assertTrue(contrast(c.statusLatest, c.statusLatestContainer) >= 4.5)
    }

    @Test
    fun `границы светлой темы различимы на своём фоне`() {
        // Порог 1.3 — не WCAG-требование (WCAG 1.4.11 к декоративным обводкам
        // не применяется), а sanity-check «не слились с фоном визуально».
        // Фактические значения: panelStroke (#AFC3BD) на ground ≈ 1.76:1,
        // track (#C8D6D1) на ground ≈ 1.43:1 — оба заметно ниже 3:1 из
        // WCAG 1.4.11, но панель различима по заливке (#FFFFFF против
        // #F8FAF7 у ground), а не по обводке, поэтому 3:1 здесь не требуется.
        val c = volcanicLightColors()
        assertTrue(contrast(c.panelStroke, c.ground) >= 1.3) { "stroke панели на фоне" }
        assertTrue(contrast(c.track, c.ground) >= 1.3) { "трек на фоне" }
    }
}
