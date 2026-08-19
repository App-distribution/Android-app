package com.appdist.feature.builddetail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.ui.components.MetaRow
import com.appdist.core.ui.components.VolcanicCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Технические детали свёрнуты по умолчанию: версия, окружение, ветка и номер
 * видны выше, а хэши и SDK нужны не в первом прочтении экрана.
 */
@Composable
fun TechDetailsSection(build: BuildUi, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val dateStr = remember(build.uploadDate) {
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(build.uploadDate))
    }

    VolcanicCard(modifier = modifier, cornerRadius = 18.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = if (expanded) {
                        "Свернуть технические детали"
                    } else {
                        "Развернуть технические детали"
                    }
                }
                .clickable { expanded = !expanded }
                .padding(start = 14.dp, end = 6.dp),
        ) {
            Text("Технические детали", style = MaterialTheme.typography.titleMedium)
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetaRow("versionCode", "${build.versionCode}")
                build.branch?.takeIf { it.isNotBlank() }?.let { MetaRow("branch", it) }
                build.commitHash?.takeIf { it.isNotBlank() }?.let { MetaRow("commit", it.take(12)) }
                MetaRow("size", "${build.fileSize / 1024 / 1024} MB")
                MetaRow("SDK", "${build.minSdk} / ${build.targetSdk}")
                if (build.abis.isNotEmpty()) MetaRow("ABIs", build.abis.joinToString(", "))
                MetaRow("SHA-256", "${build.checksumSha256.take(10)}…")
                if (build.uploaderName.isNotBlank()) MetaRow("uploader", build.uploaderName)
                MetaRow("uploaded", dateStr)
            }
        }
    }
}
