package com.appdist.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.MonoAction
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.VolcanicAccent
import com.appdist.core.ui.theme.VolcanicAccentBright
import com.appdist.core.ui.theme.VolcanicAction
import com.appdist.core.ui.theme.VolcanicGraph
import com.appdist.core.ui.theme.VolcanicGround
import com.appdist.core.ui.theme.VolcanicInk
import com.appdist.core.ui.theme.VolcanicNodeOutline
import com.appdist.core.ui.theme.VolcanicNodeRelated
import com.appdist.core.ui.theme.VolcanicRailDivider
import com.appdist.core.ui.theme.VolcanicRailTarget
import com.appdist.core.ui.theme.VolcanicRailTargetStroke

/*
THESIS: AppDist is a living build journal, expressed as a navigable release graph.
OWN-WORLD: Volcanic black ground, teal action seam, mint route light, rounded industrial panels.
STORY: choose a journal → inspect its build line → download or install → revisit downloaded artifacts.
FIRST VIEWPORT: title, current route, newest useful object, and one thumb-reachable action.
FORM: a persistent handed action zone owns the screen's primary action; planar edges never cross.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md.
*/

enum class ActionZoneSide { Left, Right }

enum class GraphDestination {
    Journals,
    Builds,
    BuildDetail,
    Downloads,
    Settings,
}

/** Ширина action-зоны и её контролов — единый размер для всех экранов. */
val ActionZoneWidth = 92.dp
val ActionTileWidth = 76.dp

/** Расстояние от низа экрана до плитки действия — как в макете. */
private val ActionZoneBottomGap = 64.dp

/**
 * Дополнительный нижний отступ поверх уже применённой системной вставки: он
 * доводит расстояние от низа экрана до [minGap], но никогда не оставляет
 * меньше [margin] над вставкой.
 *
 * Одной navigationBarsPadding() недостаточно: часть прошивок сообщает вставку
 * меньше, чем реально занимает панель навигации кнопками, и контрол уезжает
 * под неё. Значение по умолчанию перекрывает стандартные 48dp такой панели.
 */
@Composable
fun bottomGapAfterInsets(minGap: Dp = 56.dp, margin: Dp = 16.dp): Dp {
    val inset = with(LocalDensity.current) {
        WindowInsets.safeDrawing.getBottom(this).toDp()
    }
    return maxOf(minGap - inset, margin)
}

/**
 * @param zoneTop контекст зоны: 48dp-таргет и метаданные текущего объекта.
 *   Прижимается к верху, вертикальные распорки с weight внутри не нужны.
 * @param zoneAction основное действие экрана — подпись и ink-плитки. Стоит по
 *   центру свободного места зоны.
 */
@Composable
fun VolcanicScaffold(
    side: ActionZoneSide,
    destination: GraphDestination,
    zoneAction: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    zoneTop: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides VolcanicInk) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(VolcanicGround),
        ) {
            if (side == ActionZoneSide.Left) {
                ActionZoneContainer(side, zoneTop, zoneAction)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                NavigationGraphBackground(
                    destination = destination,
                    mirrored = side == ActionZoneSide.Left,
                )
                content()
            }
            if (side == ActionZoneSide.Right) {
                ActionZoneContainer(side, zoneTop, zoneAction)
            }
        }
    }
}

@Composable
private fun ActionZoneContainer(
    side: ActionZoneSide,
    zoneTop: @Composable ColumnScope.() -> Unit,
    zoneAction: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (side == ActionZoneSide.Right) {
        RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp)
    } else {
        RoundedCornerShape(topEnd = 40.dp, bottomEnd = 40.dp)
    }

    Column(
        modifier = Modifier
            .width(ActionZoneWidth)
            .fillMaxHeight()
            .background(VolcanicAction, shape)
            // safeDrawing, а не системные бары по отдельности: сюда же попадает
            // вырез и боковая панель навигации в альбомной ориентации.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 14.dp,
                bottom = bottomGapAfterInsets(minGap = ActionZoneBottomGap),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        zoneTop()
        // Равные распорки: блок действий встаёт по центру места, оставшегося
        // под контекстом, и не зависит от того, сколько метаданных сверху.
        Spacer(Modifier.weight(1f))
        zoneAction()
        Spacer(Modifier.weight(1f))
    }
}

// region Action zone primitives

/**
 * Второстепенный контрол зоны (настройки, назад, поделиться): 48dp-таргет,
 * на тон глубже самой зоны, чтобы ink-плитка действия оставалась громче всего.
 */
@Composable
fun RailIconTarget(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(VolcanicRailTarget, RoundedCornerShape(16.dp))
            .border(1.dp, VolcanicRailTargetStroke, RoundedCornerShape(16.dp))
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = VolcanicInk, modifier = Modifier.size(21.dp))
    }
}

/** Капсовая моно-подпись в зоне: LATEST, INTERNAL, LOCAL, QA. */
@Composable
fun RailLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MonoLabel,
        color = VolcanicInk,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Крупное числовое значение в зоне: количество, размер, дата. */
@Composable
fun RailValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = VolcanicInk,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun RailDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(48.dp)
            .height(1.dp)
            .background(VolcanicRailDivider),
    )
}

@Composable
fun RailDot(modifier: Modifier = Modifier, color: Color = VolcanicAccentBright) {
    Box(
        modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

/** Подпись над блоком действий: ДЕЙСТВИЕ / ЗАГРУЗКА. */
@Composable
fun ActionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MonoLabel,
        color = VolcanicAccent,
        textAlign = TextAlign.Center,
        modifier = modifier.width(ActionTileWidth),
    )
}

/**
 * Основное действие экрана: ink-плитка в нижней трети зоны, в зоне досягаемости
 * большого пальца. Содержимое задаётся слотом, чтобы прогресс загрузки жил
 * в той же плитке, а не превращался в отдельный контрол.
 */
@Composable
fun RailActionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    minHeight: Dp = 112.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) VolcanicInk else VolcanicRailTarget,
        contentColor = if (enabled) VolcanicAction else VolcanicInk.copy(alpha = .45f),
        modifier = modifier
            .width(ActionTileWidth)
            .heightIn(min = minHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Button
                    }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            content = content,
        )
    }
}

@Composable
fun RailAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 112.dp,
) {
    RailActionTile(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentDescription = label,
        minHeight = minHeight,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            text = label.uppercase(),
            style = MonoAction,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

// endregion

// region Navigation graph

/**
 * Планарный граф навигации в фоне. Координаты заданы в системе макета
 * 298 × 844 (контентная половина экрана) и масштабируются под реальный размер.
 * Рёбра сходятся только в явной развилке и никогда не пересекаются.
 */
private const val GraphDesignWidth = 298f
private const val GraphDesignHeight = 844f

private val NodeApps = Offset(258f, 150f)
private val NodeJunction = Offset(250f, 220f)
private val CornerBuilds = Offset(225f, 250f)
private val NodeBuilds = Offset(225f, 304f)
private val NodeDetail = Offset(225f, 452f)
private val CornerDownloads = Offset(195f, 260f)
private val NodeDownloads = Offset(195f, 620f)
private val CornerSettings = Offset(270f, 260f)
private val NodeSettings = Offset(270f, 734f)

private enum class NodeState { Active, Related, Idle }

@Composable
private fun NavigationGraphBackground(destination: GraphDestination, mirrored: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val sx = size.width / GraphDesignWidth
        val sy = size.height / GraphDesignHeight
        fun p(point: Offset): Offset {
            val x = if (mirrored) GraphDesignWidth - point.x else point.x
            return Offset(x * sx, point.y * sy)
        }

        val base = Path().apply {
            polyline(listOf(p(NodeApps), p(NodeJunction)))
            polyline(listOf(p(NodeJunction), p(CornerBuilds), p(NodeDetail)))
            polyline(listOf(p(NodeJunction), p(CornerDownloads), p(NodeDownloads)))
            polyline(listOf(p(NodeJunction), p(CornerSettings), p(NodeSettings)))
        }
        drawPath(base, VolcanicGraph, style = graphStroke(2.dp.toPx()))

        val active = Path().apply {
            when (destination) {
                GraphDestination.Journals -> {
                    polyline(listOf(p(NodeApps), p(NodeJunction), p(CornerBuilds), p(NodeBuilds)))
                    polyline(listOf(p(NodeJunction), p(CornerDownloads), p(NodeDownloads)))
                }
                GraphDestination.Builds ->
                    polyline(listOf(p(NodeApps), p(NodeJunction), p(CornerBuilds), p(NodeBuilds)))
                GraphDestination.BuildDetail ->
                    polyline(listOf(p(NodeApps), p(NodeJunction), p(CornerBuilds), p(NodeDetail)))
                GraphDestination.Downloads ->
                    polyline(listOf(p(NodeApps), p(NodeJunction), p(CornerDownloads), p(NodeDownloads)))
                GraphDestination.Settings ->
                    polyline(listOf(p(NodeApps), p(NodeJunction), p(CornerSettings), p(NodeSettings)))
            }
        }
        drawPath(active, VolcanicAccent.copy(alpha = .6f), style = graphStroke(2.5.dp.toPx()))

        drawCircle(VolcanicAccent, 3.dp.toPx(), p(NodeJunction))
        drawNode(p(NodeApps), destination.stateOf(GraphDestination.Journals))
        drawNode(p(NodeBuilds), destination.stateOf(GraphDestination.Builds))
        drawNode(p(NodeDetail), destination.stateOf(GraphDestination.BuildDetail))
        drawNode(p(NodeDownloads), destination.stateOf(GraphDestination.Downloads))
        drawNode(p(NodeSettings), destination.stateOf(GraphDestination.Settings))
    }
}

/**
 * Узел подсвечен, если это текущий пункт; «связан», если лежит на активном
 * маршруте или доступен с текущего экрана одним действием.
 */
private fun GraphDestination.stateOf(node: GraphDestination): NodeState = when {
    this == node -> NodeState.Active
    node == GraphDestination.Journals -> NodeState.Related
    this == GraphDestination.Journals && node == GraphDestination.Builds -> NodeState.Related
    this == GraphDestination.Journals && node == GraphDestination.Downloads -> NodeState.Related
    this == GraphDestination.BuildDetail && node == GraphDestination.Builds -> NodeState.Related
    else -> NodeState.Idle
}

private fun DrawScope.drawNode(center: Offset, state: NodeState) {
    when (state) {
        NodeState.Active -> {
            drawCircle(VolcanicAccent, 7.dp.toPx(), center)
            drawCircle(VolcanicAccentBright, 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
        NodeState.Related -> {
            drawCircle(VolcanicNodeRelated, 5.dp.toPx(), center)
            drawCircle(VolcanicAccent, 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
        NodeState.Idle -> {
            drawCircle(VolcanicGround, 5.dp.toPx(), center)
            drawCircle(VolcanicNodeOutline, 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
    }
}

private fun graphStroke(width: Float) =
    Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun Path.polyline(points: List<Offset>) {
    points.forEachIndexed { index, point ->
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
}

// endregion

/**
 * Фирменный знак: тот же граф с журналом сборок, что и в иконке приложения.
 * Координаты нормированы по стороне квадрата иконки (320).
 */
@Composable
fun GraphMark(modifier: Modifier = Modifier, markSize: Dp = 52.dp) {
    Canvas(modifier.size(markSize)) {
        val s = size.minDimension / 320f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        val apps = p(208f, 58f)
        val junction = p(198f, 110f)
        val logTop = p(162f, 144f)
        val logLatest = p(162f, 242f)
        val downloadsCorner = p(110f, 152f)
        val downloads = p(110f, 260f)
        val settingsCorner = p(250f, 152f)
        val settings = p(250f, 260f)

        val base = Path().apply {
            polyline(listOf(junction, downloadsCorner, downloads))
            polyline(listOf(junction, settingsCorner, settings))
        }
        drawPath(base, VolcanicInk.copy(alpha = .45f), style = graphStroke(8f * s))

        val route = Path().apply { polyline(listOf(apps, junction, logTop, logLatest)) }
        drawPath(route, VolcanicAccent, style = graphStroke(11f * s))

        drawCircle(VolcanicNodeRelated, 10f * s, apps)
        drawCircle(VolcanicAccentBright, 10f * s, apps, style = Stroke(4f * s))
        drawCircle(VolcanicInk, 7f * s, junction)
        drawCircle(VolcanicNodeRelated, 9f * s, logTop)
        drawCircle(VolcanicAccent, 9f * s, logTop, style = Stroke(4f * s))
        listOf(p(162f, 176f), p(162f, 208f)).forEach { node ->
            drawCircle(VolcanicGround, 8f * s, node)
            drawCircle(VolcanicAccent, 8f * s, node, style = Stroke(3f * s))
        }
        drawCircle(VolcanicAccent, 13f * s, logLatest)
        drawCircle(VolcanicAccentBright, 13f * s, logLatest, style = Stroke(4f * s))
        listOf(downloads, settings).forEach { node ->
            drawCircle(VolcanicGround, 9f * s, node)
            drawCircle(VolcanicInk.copy(alpha = .6f), 9f * s, node, style = Stroke(4f * s))
        }
    }
}
