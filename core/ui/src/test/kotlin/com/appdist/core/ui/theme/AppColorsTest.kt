package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Снимки палитр и выведенной из них ColorScheme. Тёмные значения перенесены
 * из Color.kt до миграции на токены: тест падает, если тёмная тема поехала
 * при правках светлой. Светлая палитра и тёмная ColorScheme зафиксированы
 * так же — любое расхождение с текущими значениями должно быть осознанным.
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
        assertEquals(Color(0xFFC9FFF5), c.railDot)
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

    /**
     * Снимок светлой палитры, симметричный тёмному: фиксирует все 37 токенов
     * (+ railDot из Fix 1) как они есть сейчас, чтобы правки светлой темы
     * были осознанными, а не побочным эффектом.
     */
    @Test
    fun `светлая палитра совпадает со снимком`() {
        val c = volcanicLightColors()
        assertEquals(Color(0xFFF8FAF7), c.ground)
        assertEquals(Color(0xFFFFFFFF), c.surface)
        assertEquals(Color(0xFFEFF5F2), c.surfaceRaised)
        assertEquals(Color(0xF2FFFFFF), c.panel)
        assertEquals(Color(0xFFAFC3BD), c.panelStroke)
        assertEquals(Color(0xFF9DB2AC), c.panelStrokeStrong)
        assertEquals(Color(0xFFFFFFFF), c.field)
        assertEquals(Color(0xFFFFFFFF), c.chip)
        assertEquals(Color(0xFFC8D6D1), c.track)
        assertEquals(Color(0xFFAFC3BD), c.outline)
        assertEquals(Color(0xFF132D29), c.ink)
        assertEquals(Color(0xFF334D47), c.inkSoft)
        assertEquals(Color(0xFF516A65), c.muted)
        assertEquals(Color(0xFF5B736D), c.mutedDim)
        assertEquals(Color(0x0D0B3F3C), c.ghost)
        assertEquals(Color(0x665B736D), c.ghostLabel)
        assertEquals(Color(0xFF0B756C), c.accent)
        assertEquals(Color(0xFFF4FFFC), c.accentBright)
        assertEquals(Color(0xFFD6ECE7), c.action)
        assertEquals(Color(0xFF132D29), c.actionTile)
        assertEquals(Color(0xFFFFFFFF), c.onActionTile)
        assertEquals(Color(0xFFC4E0DA), c.railTarget)
        assertEquals(Color(0xFFC5D3CF), c.railTargetStroke)
        assertEquals(Color(0xFF9DB2AC), c.railDivider)
        assertEquals(Color(0xFF0B756C), c.railDot)
        assertEquals(Color(0x3F819C96), c.graph)
        assertEquals(Color(0xFF84B6AD), c.nodeRelated)
        assertEquals(Color(0x8A6F8F88), c.nodeOutline)
        assertEquals(Color(0xFFEFF5F2), c.previewGround)
        assertEquals(Color(0xFF0B756C), c.actionPreview)
        assertEquals(Color(0xFF8C1C00), c.statusMandatory)
        assertEquals(Color(0xFFF0DEDA), c.statusMandatoryContainer)
        assertEquals(Color(0xFF4A5754), c.statusDeprecated)
        assertEquals(Color(0xFFE4E9E7), c.statusDeprecatedContainer)
        assertEquals(Color(0xFF804200), c.statusExpiring)
        assertEquals(Color(0xFFF2E6D3), c.statusExpiringContainer)
        assertEquals(Color(0xFF0B756C), c.statusLatest)
        assertEquals(Color(0xFFD6ECE7), c.statusLatestContainer)
    }

    @Test
    fun `светлая палитра отличается от тёмной по фону и тексту`() {
        val dark = volcanicDarkColors()
        val light = volcanicLightColors()
        assert(light.ground != dark.ground)
        assert(light.ink != dark.ink)
    }

    /**
     * Снимок Material ColorScheme, который toColorScheme выводит из тёмной
     * палитры. Без этого теста подстановка токенов в слоты ColorScheme может
     * незаметно поехать — как уже случилось с onPrimary, primaryContainer,
     * onPrimaryContainer и outlineVariant при миграции на токены: у них нет
     * видимых потребителей, поэтому визуально ничего не ломалось.
     */
    @Test
    fun `тёмная ColorScheme совпадает со снимком`() {
        val c = volcanicDarkColors()
        val scheme = c.toColorScheme(darkTheme = true)

        assertEquals(c.accent, scheme.primary)
        assertEquals(c.ground, scheme.onPrimary)
        assertEquals(c.action, scheme.primaryContainer)
        assertEquals(c.ink, scheme.onPrimaryContainer)
        assertEquals(c.surfaceRaised, scheme.secondaryContainer)
        assertEquals(c.ink, scheme.onSecondaryContainer)
        assertEquals(c.ground, scheme.background)
        assertEquals(c.ink, scheme.onBackground)
        assertEquals(c.surface, scheme.surface)
        assertEquals(c.ink, scheme.onSurface)
        assertEquals(c.surfaceRaised, scheme.surfaceVariant)
        assertEquals(c.muted, scheme.onSurfaceVariant)
        assertEquals(c.outline, scheme.outline)
        assertEquals(c.outline, scheme.outlineVariant)
        // Fix 2: AlertDialog берёт контейнер из surfaceContainerHigh — семейство
        // surfaceContainer теперь тоже выведено из токенов, а не из дефолта Material.
        // Это единственное осознанное изменение тёмной темы во всей волне правок.
        assertEquals(c.ground, scheme.surfaceContainerLowest)
        assertEquals(c.surface, scheme.surfaceContainerLow)
        assertEquals(c.surface, scheme.surfaceContainer)
        assertEquals(c.surfaceRaised, scheme.surfaceContainerHigh)
        assertEquals(c.surfaceRaised, scheme.surfaceContainerHighest)
        assertEquals(c.accent, scheme.surfaceTint)
    }
}
