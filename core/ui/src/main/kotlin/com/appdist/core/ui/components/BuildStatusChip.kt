package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.AppTheme

enum class BuildChipType {
    MANDATORY, LATEST, DEPRECATED, EXPIRING, CHANNEL, ENVIRONMENT
}

@Composable
fun BuildStatusChip(
    label: String,
    type: BuildChipType,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val (bg, fg) = when (type) {
        BuildChipType.MANDATORY -> colors.statusMandatoryContainer to colors.statusMandatory
        BuildChipType.LATEST -> colors.statusLatestContainer to colors.statusLatest
        BuildChipType.DEPRECATED -> colors.statusDeprecatedContainer to colors.statusDeprecated
        BuildChipType.EXPIRING -> colors.statusExpiringContainer to colors.statusExpiring
        BuildChipType.CHANNEL, BuildChipType.ENVIRONMENT ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = label.uppercase(),
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
