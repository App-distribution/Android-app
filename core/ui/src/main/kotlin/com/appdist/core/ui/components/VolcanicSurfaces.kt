package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.core.ui.theme.MonoValue
import com.appdist.core.ui.theme.VolcanicAccent
import com.appdist.core.ui.theme.VolcanicActionPreview
import com.appdist.core.ui.theme.VolcanicField
import com.appdist.core.ui.theme.VolcanicGhost
import com.appdist.core.ui.theme.VolcanicInk
import com.appdist.core.ui.theme.VolcanicMutedDim
import com.appdist.core.ui.theme.VolcanicPanel
import com.appdist.core.ui.theme.VolcanicPanelStroke
import com.appdist.core.ui.theme.VolcanicTrack

/**
 * Панель журнала: полупрозрачный базальт с тонкой светлой рамкой. Граф
 * навигации продолжает читаться под ней, поэтому фон не заливается наглухо.
 */
@Composable
fun VolcanicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    fill: Color = VolcanicPanel,
    stroke: Color = VolcanicPanelStroke,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fill, shape)
            .border(1.dp, stroke, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/**
 * Гигантская цифра журнала в фоне: номер сборки, счётчик приложений или
 * загрузок. Второе прочтение экрана, а не декор — цифра всегда о содержимом.
 */
@Composable
fun GhostNumeral(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 128.sp,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            lineHeight = fontSize,
            letterSpacing = (-4).sp,
        ),
        color = VolcanicGhost,
        modifier = modifier,
    )
}

/** Разделитель внутри панели: только тон, без второй рамки. */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VolcanicPanelStroke),
    )
}

/** Подпись под гигантской цифрой: BUILD 6737, BR:dev3 / APK. */
@Composable
fun GhostLabel(text: String, modifier: Modifier = Modifier, color: Color) {
    Text(text = text, style = MonoLabel, color = color, modifier = modifier)
}

data class RouteTab(val label: String, val selected: Boolean, val onClick: () -> Unit)

/**
 * Переключение верхнеуровневых маршрутов живёт в контенте, а не в зоне
 * действия: зона принадлежит основному действию экрана.
 */
@Composable
fun RouteSwitcher(tabs: List<RouteTab>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab -> RouteTabItem(tab) }
    }
}

@Composable
private fun RouteTabItem(tab: RouteTab) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // Ширина по тексту: иначе первая вкладка забирает всю строку, а её
        // подчёркивание растягивается на весь экран.
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .semantics {
                role = Role.Tab
                selected = tab.selected
            }
            .clickable(onClick = tab.onClick)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = tab.label.uppercase(),
            style = MonoLabel,
            color = if (tab.selected) VolcanicInk else VolcanicMutedDim,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (tab.selected) VolcanicAccent else Color.Transparent),
        )
    }
}

/**
 * Дорожка занятого места: локальные APK против свободного хранилища. Ненулевая
 * доля всегда рисуется видимым отрезком — иначе дорожка читается как пустая,
 * хотя файлы на диске есть.
 */
@Composable
fun StorageBar(fraction: Float, modifier: Modifier = Modifier) {
    val filled = fraction.coerceIn(0f, 1f).let { if (it > 0f) it.coerceAtLeast(0.04f) else 0f }
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(VolcanicTrack, RoundedCornerShape(4.dp)),
    ) {
        if (filled > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(filled)
                    .height(8.dp)
                    .background(VolcanicAccent, RoundedCornerShape(4.dp)),
            )
        }
    }
}

/**
 * Зона досягаемости большого пальца. Подсвеченный отрезок стоит со стороны
 * рабочей руки — иначе схема противоречит выбранному положению action-зоны.
 */
@Composable
fun ReachBar(
    modifier: Modifier = Modifier,
    alignStart: Boolean = false,
    sweetSpotFraction: Float = 0.46f,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(VolcanicTrack, RoundedCornerShape(4.dp)),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .fillMaxWidth(sweetSpotFraction)
                .height(8.dp)
                .background(VolcanicAccent, RoundedCornerShape(4.dp)),
        )
    }
}

/** Мини-превью телефона с подсвеченной стороной action-зоны. */
@Composable
fun HandednessPreview(onLeft: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .width(34.dp)
            .height(52.dp)
            .clip(shape)
            .background(VolcanicField)
            .border(1.dp, VolcanicPanelStroke, shape),
        contentAlignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .width(10.dp)
                .height(52.dp)
                .background(
                    VolcanicActionPreview,
                    if (onLeft) {
                        RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                    } else {
                        RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                    },
                ),
        )
    }
}

/** Строка «ключ — значение» в техническом блоке: моно с обеих сторон. */
@Composable
fun MetaRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MonoMeta,
            color = VolcanicMutedDim,
            modifier = Modifier.weight(.42f),
        )
        Text(
            text = value,
            style = MonoValue,
            color = VolcanicInk,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.58f),
        )
    }
}
