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
    val highlightInk: Color,
    val highlightStroke: Color,
    val badgeStroke: Color,
    val listDivider: Color,
    val progress: Color,
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

/** Тёплая графитовая версия Clean Ledger для тёмного режима. */
fun volcanicDarkColors(): AppColors = AppColors(
    ground = Color(0xFF17161A),
    surface = Color(0xFF232127),
    surfaceRaised = Color(0xFF2B2930),
    panel = Color(0xFF232127),
    panelStroke = Color(0xFF3C3941),
    panelStrokeStrong = Color(0xFF4A4550),
    field = Color(0xFF232127),
    chip = Color(0xFF232127),
    track = Color(0xFF39363D),
    outline = Color(0xFF3C3941),
    ink = Color(0xFFF3F0F2),
    inkSoft = Color(0xFFD6D0D6),
    muted = Color(0xFFB2ABB3),
    mutedDim = Color(0xFFB2ABB3),
    ghost = Color(0x00FFFFFF),
    ghostLabel = Color(0x00FFFFFF),
    accent = Color(0xFFC9C7DB),
    accentBright = Color(0xFF34313B),
    highlightInk = Color(0xFFC9C7DB),
    highlightStroke = Color(0xFF4A4550),
    badgeStroke = Color(0xFF4A4550),
    listDivider = Color(0xFF3C3941),
    progress = Color(0xFF6F6A7C),
    action = Color(0xFF2C232D),
    actionTile = Color(0xFFF3F0F2),
    onActionTile = Color(0xFF232127),
    railTarget = Color(0xFF232127),
    railTargetStroke = Color(0xFF3C3941),
    railDivider = Color(0xFF3C3941),
    railDot = Color(0xFFC9C7DB),
    graph = Color(0x003C3941),
    nodeRelated = Color(0xFF6F6A7C),
    nodeOutline = Color(0xFF4A4550),
    previewGround = Color(0xFF232127),
    actionPreview = Color(0xFF6F6A7C),
    statusMandatory = Color(0xFFFFB4AB),
    statusMandatoryContainer = Color(0xFF4A1D1B),
    statusDeprecated = Color(0xFFCAD4D1),
    statusDeprecatedContainer = Color(0xFF29302F),
    statusExpiring = Color(0xFFE1C9DD),
    statusExpiringContainer = Color(0xFF3D303B),
    statusLatest = Color(0xFFF3F0F2),
    statusLatestContainer = Color(0xFF34313B),
)

/**
 * Светлая версия Clean Ledger: кремовый холст, графит и спокойный лиловый.
 */
fun volcanicLightColors(): AppColors = AppColors(
    ground = Color(0xFFFCFBF8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF8F7FA),
    panel = Color(0xFFFFFFFF),
    panelStroke = Color(0xFFD8D5CF),
    panelStrokeStrong = Color(0xFFC9C7DB),
    field = Color(0xFFFFFFFF),
    chip = Color(0xFFF1F0ED),
    track = Color(0xFFE9E6E2),
    outline = Color(0xFFD8D5CF),
    ink = Color(0xFF2B2A28),
    inkSoft = Color(0xFF4D4B55),
    muted = Color(0xFF65635F),
    mutedDim = Color(0xFF65635F),
    ghost = Color(0x002B2A28),
    ghostLabel = Color(0x002B2A28),
    accent = Color(0xFF4D4B55),
    accentBright = Color(0xFFEEF1FF),
    highlightInk = Color(0xFF3B50B8),
    highlightStroke = Color(0xFFDAD7E8),
    badgeStroke = Color(0xFFD6D4E4),
    listDivider = Color(0xFFDFDDD8),
    progress = Color(0xFFC9C7DB),
    action = Color(0xFFFFF0FC),
    actionTile = Color(0xFF2B2A28),
    onActionTile = Color(0xFFFFFFFF),
    railTarget = Color(0xFFFFFFFF),
    railTargetStroke = Color(0xFFD8D5CF),
    railDivider = Color(0xFFD8D5CF),
    railDot = Color(0xFF4D4B55),
    graph = Color(0x00D8D5CF),
    nodeRelated = Color(0xFFC9C7DB),
    nodeOutline = Color(0xFFD8D5CF),
    previewGround = Color(0xFFF8F7FA),
    actionPreview = Color(0xFFC9C7DB),
    statusMandatory = Color(0xFF8C1C00),
    statusMandatoryContainer = Color(0xFFF0DEDA),
    statusDeprecated = Color(0xFF4A5754),
    statusDeprecatedContainer = Color(0xFFE4E9E7),
    statusExpiring = Color(0xFF67526A),
    statusExpiringContainer = Color(0xFFEEE7EE),
    statusLatest = Color(0xFF343447),
    statusLatestContainer = Color(0xFFE7E6F2),
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
