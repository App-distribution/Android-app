package com.appdist.feature.builddetail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.core.ui.theme.VolcanicMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Журнал изменений сборки: сама запись проза, её обстоятельства — моно.
 */
@Composable
fun ChangelogSection(
    changelog: String,
    uploadDate: Long,
    fileSize: Long,
    modifier: Modifier = Modifier,
) {
    val meta = remember(uploadDate, fileSize) {
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uploadDate))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(uploadDate))
        "${date.uppercase()}\n$time  ·  ${fileSize / 1024 / 1024} MB"
    }

    Column(modifier) {
        Text("Что нового", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(14.dp))
        VolcanicCard(cornerRadius = 20.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(changelog, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(meta, style = MonoMeta, color = VolcanicMuted)
            }
        }
    }
}
