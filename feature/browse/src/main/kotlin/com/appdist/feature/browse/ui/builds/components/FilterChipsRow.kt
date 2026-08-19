package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.AppTheme
import com.appdist.feature.browse.ui.builds.BuildFilters

val STANDS = listOf("dev3", "l10", "prod", "stage")
val BUILD_TYPES = listOf("предрелиз", "приемочное")

/**
 * Фильтры канала и стенда. Выбранный уходит на ink-поверхность — тот же
 * контраст, что и у основного действия, поэтому состояние читается без цвета.
 */
@Composable
fun FilterChipsRow(
    filters: BuildFilters,
    onChannelSelected: (String?) -> Unit,
    onEnvironmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val noFilters = filters.environment == null && filters.channel == null

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterPill(
            label = "все",
            selected = noFilters,
            onClick = {
                onEnvironmentSelected(null)
                onChannelSelected(null)
            },
        )
        STANDS.forEach { stand ->
            FilterPill(
                label = stand,
                selected = filters.environment == stand,
                onClick = { onEnvironmentSelected(if (filters.environment == stand) null else stand) },
            )
        }
        BUILD_TYPES.forEach { type ->
            FilterPill(
                label = type,
                selected = filters.channel == type,
                onClick = { onChannelSelected(if (filters.channel == type) null else type) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 61.dp)
            .background(if (selected) AppTheme.colors.actionTile else AppTheme.colors.chip, shape)
            .border(1.dp, AppTheme.colors.panelStroke, shape)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AppTheme.colors.onActionTile else AppTheme.colors.ink,
        )
    }
}
