package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.BuildChipType
import com.appdist.core.ui.components.BuildStatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildCard(
    build: BuildUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
) {
    val dateStr = remember(build.uploadDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(build.uploadDate))
    }
    val version = build.versionName.takeIf { it.startsWith("v") } ?: "v${build.versionName}"

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (featured) 16.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (featured) 3.dp else 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(if (featured) 20.dp else 16.dp)) {
            if (featured) {
                Text(
                    text = "LATEST BUILD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = version,
                    style = if (featured) MaterialTheme.typography.displaySmall
                    else MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "build ${build.versionCode}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                when (build.installStatus) {
                    is InstallStatus.UpdateAvailable -> Badge { Text("↑") }
                    is InstallStatus.Installed -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Unit
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(if (featured) 12.dp else 10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BuildStatusChip(build.channel, BuildChipType.CHANNEL)
                if (build.isLatestInChannel) BuildStatusChip("LATEST", BuildChipType.LATEST)
                BuildStatusChip(build.environment, BuildChipType.ENVIRONMENT)
                if (build.status == "mandatory") BuildStatusChip("MANDATORY", BuildChipType.MANDATORY)
                if (build.status == "deprecated") BuildStatusChip("DEPRECATED", BuildChipType.DEPRECATED)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "$dateStr · ${build.fileSize / 1024 / 1024} MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (featured && !build.changelog.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = build.changelog.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
    }
}
