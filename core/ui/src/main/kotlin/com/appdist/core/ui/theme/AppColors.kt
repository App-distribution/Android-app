package com.appdist.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Фирменные токены Volcanic Split. Material 3 не даёт слотов под граф
 * навигации, ghost-цифры и панели, поэтому цвет живёт здесь, а colorScheme
 * выводится отсюда же — см. AppTheme.
 */
@Immutable
data class AppColors(
    // поверхности
    val ground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val panel: Color,
    val panelStroke: Color,
    val panelStrokeStrong: Color,
    val field: Color,
    val chip: Color,
    val track: Color,
    val outline: Color,
    // текст
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val mutedDim: Color,
    val ghost: Color,
    val ghostLabel: Color,
    // акцент и действие
    val accent: Color,
    val accentBright: Color,
    val action: Color,
    val actionTile: Color,
    val onActionTile: Color,
    // рельс и граф навигации
    val railTarget: Color,
    val railTargetStroke: Color,
    val railDivider: Color,
    val railDot: Color,
    val graph: Color,
    val nodeRelated: Color,
    val nodeOutline: Color,
    // превью хвата в настройках
    val previewGround: Color,
    val actionPreview: Color,
    // статусы сборок
    val statusMandatory: Color,
    val statusMandatoryContainer: Color,
    val statusDeprecated: Color,
    val statusDeprecatedContainer: Color,
    val statusExpiring: Color,
    val statusExpiringContainer: Color,
    val statusLatest: Color,
    val statusLatestContainer: Color,
)

/** Чёрная базальтовая земля, teal-шов действия, мятный свет маршрута. */
fun volcanicDarkColors(): AppColors = AppColors(
    ground = Color(0xFF080B0C),
    surface = Color(0xFF111718),
    surfaceRaised = Color(0xFF182123),
    panel = Color(0xCC10191A),
    panelStroke = Color(0x55FFFFFF),
    panelStrokeStrong = Color(0x66FFFFFF),
    field = Color(0x99050808),
    chip = Color(0xAA050808),
    track = Color(0xFF263637),
    outline = Color(0xFF30433F),
    ink = Color(0xFFECF1EF),
    inkSoft = Color(0xFFD5E0DD),
    muted = Color(0xFFA9BCB8),
    mutedDim = Color(0xFF8FA7A3),
    ghost = Color(0x08FFFFFF),
    ghostLabel = Color(0x33AFC5C0),
    accent = Color(0xFF9DDDD2),
    accentBright = Color(0xFFC9FFF5),
    action = Color(0xFF063F47),
    actionTile = Color(0xFFECF1EF),
    onActionTile = Color(0xFF063F47),
    railTarget = Color(0xFF052F35),
    railTargetStroke = Color(0x33FFFFFF),
    railDivider = Color(0x66FFFFFF),
    railDot = Color(0xFFC9FFF5),
    graph = Color(0x387FA09A),
    nodeRelated = Color(0xFF376C67),
    nodeOutline = Color(0x777FA09A),
    previewGround = Color(0x99050808),
    actionPreview = Color(0xFF0C6974),
    statusMandatory = Color(0xFFFFB4AB),
    statusMandatoryContainer = Color(0xFF4A1D1B),
    statusDeprecated = Color(0xFFCAD4D1),
    statusDeprecatedContainer = Color(0xFF29302F),
    statusExpiring = Color(0xFFFFD18A),
    statusExpiringContainer = Color(0xFF4A3210),
    statusLatest = Color(0xFF9DDDD2),
    statusLatestContainer = Color(0xFF063F47),
)

/**
 * Минерально-светлая поверхность: тот же маршрут и teal-зона, но основное
 * действие становится тёмной плиткой, а не светлой.
 */
fun volcanicLightColors(): AppColors = AppColors(
    ground = Color(0xFFF8FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEFF5F2),
    panel = Color(0xF2FFFFFF),
    panelStroke = Color(0xFFAFC3BD),
    panelStrokeStrong = Color(0xFF9DB2AC),
    field = Color(0xFFFFFFFF),
    chip = Color(0xFFFFFFFF),
    track = Color(0xFFC8D6D1),
    outline = Color(0xFFAFC3BD),
    ink = Color(0xFF132D29),
    inkSoft = Color(0xFF334D47),
    muted = Color(0xFF516A65),
    mutedDim = Color(0xFF5B736D),
    ghost = Color(0x0D0B3F3C),
    ghostLabel = Color(0x665B736D),
    accent = Color(0xFF0B756C),
    accentBright = Color(0xFFF4FFFC),
    action = Color(0xFFD6ECE7),
    actionTile = Color(0xFF132D29),
    onActionTile = Color(0xFFFFFFFF),
    railTarget = Color(0xFFC4E0DA),
    railTargetStroke = Color(0xFFC5D3CF),
    railDivider = Color(0xFF9DB2AC),
    railDot = Color(0xFF0B756C),
    graph = Color(0x3F819C96),
    nodeRelated = Color(0xFF84B6AD),
    nodeOutline = Color(0x8A6F8F88),
    previewGround = Color(0xFFEFF5F2),
    actionPreview = Color(0xFF0B756C),
    statusMandatory = Color(0xFF8C1C00),
    statusMandatoryContainer = Color(0xFFF0DEDA),
    statusDeprecated = Color(0xFF4A5754),
    statusDeprecatedContainer = Color(0xFFE4E9E7),
    statusExpiring = Color(0xFF804200),
    statusExpiringContainer = Color(0xFFF2E6D3),
    statusLatest = Color(0xFF0B756C),
    statusLatestContainer = Color(0xFFD6ECE7),
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors не предоставлены — оберните контент в AppTheme")
}

/** Точка доступа к токенам: AppTheme.colors.ink и т.д. */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
