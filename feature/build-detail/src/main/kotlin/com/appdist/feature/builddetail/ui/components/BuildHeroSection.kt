package com.appdist.feature.builddetail.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.ui.components.BuildChipType
import com.appdist.core.ui.components.BuildStatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildHeroSection(build: BuildUi, modifier: Modifier = Modifier) {
    val date = remember(build.uploadDate) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(build.uploadDate))
    }
    val version = build.versionName.takeIf { it.startsWith("v") } ?: "v${build.versionName}"

    Column(modifier) {
        Text(
            text = "LATEST BUILD",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = version,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "build ${build.versionCode}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuildStatusChip(build.channel, BuildChipType.CHANNEL)
            if (build.buildType.isNotBlank()) {
                BuildStatusChip(build.buildType, BuildChipType.CHANNEL)
            }
            BuildStatusChip(build.environment, BuildChipType.ENVIRONMENT)
            if (build.isLatestInChannel) BuildStatusChip("LATEST", BuildChipType.LATEST)
            if (build.status == "mandatory") BuildStatusChip("MANDATORY", BuildChipType.MANDATORY)
        }

        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                Column {
                    Text(
                        text = "ЗАГРУЖЕНО",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "${build.fileSize / 1024 / 1024} MB",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
