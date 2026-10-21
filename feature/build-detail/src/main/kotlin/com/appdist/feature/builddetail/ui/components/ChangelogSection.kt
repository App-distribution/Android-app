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
import com.appdist.core.ui.components.LedgerCard
import com.appdist.core.ui.components.LedgerSectionLabel
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta
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
        LedgerCard(highlighted = true) {
            Column(Modifier.padding(16.dp)) {
                LedgerSectionLabel("Что нового")
                Spacer(Modifier.height(14.dp))
                Text(changelog, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(18.dp))
                Text(meta, style = MonoMeta, color = AppTheme.colors.muted)
            }
        }
    }
}
