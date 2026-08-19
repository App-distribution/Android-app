package com.appdist.feature.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.GhostNumeral
import com.appdist.core.ui.components.bottomGapAfterInsets
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.HandednessPreview
import com.appdist.core.ui.components.RailAction
import com.appdist.core.ui.components.RailLabel
import com.appdist.core.ui.components.ReachBar
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.volcanicDarkColors
import com.appdist.core.ui.theme.volcanicLightColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPermissionsClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val side = if (state.actionZoneOnLeft) ActionZoneSide.Left else ActionZoneSide.Right

    VolcanicScaffold(
        side = side,
        destination = GraphDestination.Settings,
        zoneTop = {
            RailLabel(if (state.actionZoneOnLeft) "LEFT" else "RIGHT")
        },
        zoneAction = {
            ActionCaption("ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            RailAction(
                icon = Icons.Default.Check,
                label = "Применить",
                onClick = onBack,
                minHeight = 126.dp,
            )
        },
    ) {
        GhostNumeral(
            text = if (state.actionZoneOnLeft) "L" else "R",
            fontSize = 170.sp,
            modifier = Modifier
                .align(if (state.actionZoneOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(horizontal = 48.dp, vertical = 24.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 16.dp, bottom = bottomGapAfterInsets()),
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(48.dp)
                    .background(AppTheme.colors.panel, RoundedCornerShape(16.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }

            Spacer(Modifier.height(14.dp))
            Text("Настройки", style = MaterialTheme.typography.displaySmall)

            Spacer(Modifier.height(24.dp))
            Text(
                "Тема",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.muted,
            )
            Spacer(Modifier.height(14.dp))
            ThemeChoice(
                label = "Как в системе",
                hint = "следовать настройке телефона",
                mode = ThemeMode.SYSTEM,
                selected = state.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            Spacer(Modifier.height(12.dp))
            ThemeChoice(
                label = "Светлая",
                hint = "минеральная поверхность",
                mode = ThemeMode.LIGHT,
                selected = state.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            Spacer(Modifier.height(12.dp))
            ThemeChoice(
                label = "Тёмная",
                hint = "базальтовая земля",
                mode = ThemeMode.DARK,
                selected = state.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "Положение action-зоны",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.muted,
            )
            Spacer(Modifier.height(14.dp))
            PlacementChoice(
                label = "Справа",
                hint = "для правой руки",
                selected = !state.actionZoneOnLeft,
                onLeft = false,
                onClick = { viewModel.setActionZoneOnLeft(false) },
            )
            Spacer(Modifier.height(12.dp))
            PlacementChoice(
                label = "Слева",
                hint = "для левой руки",
                selected = state.actionZoneOnLeft,
                onLeft = true,
                onClick = { viewModel.setActionZoneOnLeft(true) },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Основное действие переместится вместе с зоной. Содержимое и порядок экранов не изменятся.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted,
            )

            Spacer(Modifier.height(28.dp))
            Text("ЗОНА ДОСЯГАЕМОСТИ", style = MonoLabel, color = AppTheme.colors.accent)
            Spacer(Modifier.height(12.dp))
            ReachBar(alignStart = state.actionZoneOnLeft)
            Spacer(Modifier.height(10.dp))
            Text(
                "Основные действия собраны в action-зоне со стороны рабочей руки, по центру экрана — до них дотягиваешься, не перехватывая телефон.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.mutedDim,
            )

            Spacer(Modifier.height(30.dp))
            OutlinedButton(
                onClick = onPermissionsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Разрешения установки")
            }
        }
    }
}

/**
 * Выбор стороны показывает результат до применения: мини-превью телефона с
 * подсвеченной зоной, а не только подпись.
 */
@Composable
private fun PlacementChoice(
    label: String,
    hint: String,
    selected: Boolean,
    onLeft: Boolean,
    onClick: () -> Unit,
) {
    VolcanicCard(
        cornerRadius = 18.dp,
        stroke = if (selected) AppTheme.colors.accent else AppTheme.colors.panelStroke,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ChoiceDot(selected)
            Column(Modifier
                .weight(1f)
                .padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.mutedDim)
            }
            HandednessPreview(onLeft = onLeft)
        }
    }
}

@Composable
private fun ChoiceDot(selected: Boolean) {
    if (selected) {
        Box(
            Modifier
                .size(16.dp)
                .background(AppTheme.colors.accent, CircleShape),
        )
    } else {
        Box(
            Modifier
                .size(16.dp)
                .background(AppTheme.colors.ground, CircleShape)
                .border(2.dp, AppTheme.colors.mutedDim, CircleShape),
        )
    }
}

/**
 * Выбор темы повторяет язык выбора хвата: точка, подпись и мини-превью
 * телефона, залитого фоном соответствующей темы.
 */
@Composable
private fun ThemeChoice(
    label: String,
    hint: String,
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    VolcanicCard(
        cornerRadius = 18.dp,
        stroke = if (selected) AppTheme.colors.accent else AppTheme.colors.panelStroke,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ChoiceDot(selected)
            Column(Modifier
                .weight(1f)
                .padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.mutedDim)
            }
            ThemeSwatch(mode)
        }
    }
}

/** Мини-превью: сплошная заливка для светлой и тёмной, диагональ — для системной. */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val shape = RoundedCornerShape(10.dp)
    val dark = volcanicDarkColors().ground
    val light = volcanicLightColors().ground
    val stroke = AppTheme.colors.panelStroke
    Box(
        Modifier
            .width(34.dp)
            .height(52.dp)
            .clip(shape)
            .border(1.dp, stroke, shape),
    ) {
        when (mode) {
            ThemeMode.LIGHT -> Box(Modifier.fillMaxSize().background(light))
            ThemeMode.DARK -> Box(Modifier.fillMaxSize().background(dark))
            ThemeMode.SYSTEM -> {
                Box(Modifier.fillMaxSize().background(light))
                Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, dark)
                }
            }
        }
    }
}
