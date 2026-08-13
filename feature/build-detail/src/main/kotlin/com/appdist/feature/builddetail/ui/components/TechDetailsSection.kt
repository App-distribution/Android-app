package com.appdist.feature.builddetail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TechDetailsSection(build: BuildUi, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Технические детали", style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                val dateStr = remember(build.uploadDate) {
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(build.uploadDate))
                }
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DetailRow("versionCode", "${build.versionCode}")
                    build.branch?.let { DetailRow("branch", it) }
                    build.commitHash?.let { DetailRow("commit", it.take(12)) }
                    DetailRow("size", "${build.fileSize / 1024 / 1024} MB")
                    DetailRow("minSdk / targetSdk", "${build.minSdk} / ${build.targetSdk}")
                    if (build.abis.isNotEmpty()) DetailRow("ABIs", build.abis.joinToString(", "))
                    DetailRow("SHA-256", "${build.checksumSha256.take(16)}...")
                    if (build.uploaderName.isNotBlank()) DetailRow("uploader", build.uploaderName)
                    DetailRow("uploaded", dateStr)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
