package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.appdist.feature.browse.ui.builds.BuildFilters

val STANDS = listOf("dev3", "l10", "prod", "stage")
val BUILD_TYPES = listOf("предрелиз", "приемочное")

@Composable
fun FilterChipsRow(
    filters: BuildFilters,
    onChannelSelected: (String?) -> Unit,
    onEnvironmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        STANDS.forEach { stand ->
            FilterChip(
                selected = filters.environment == stand,
                onClick = {
                    onEnvironmentSelected(if (filters.environment == stand) null else stand)
                },
                label = { Text(stand, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(9.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.height(34.dp),
            )
        }
        BUILD_TYPES.forEach { type ->
            FilterChip(
                selected = filters.channel == type,
                onClick = { onChannelSelected(if (filters.channel == type) null else type) },
                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(9.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.height(34.dp),
            )
        }
    }
}
