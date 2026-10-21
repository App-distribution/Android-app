package com.appdist.feature.builddetail.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.components.LedgerCard
import com.appdist.core.ui.components.LedgerSectionLabel
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Столько строк описания видно сразу; остальное — по «Показать полностью». */
private const val COLLAPSED_LINES = 6

/**
 * Журнал изменений сборки: сама запись проза, её обстоятельства — моно.
 *
 * Описание приходит из CI целиком и бывает на несколько экранов, поэтому
 * свёрнуто до [COLLAPSED_LINES] строк — иначе техданные и кнопка установки
 * уезжают далеко вниз.
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
    var expanded by rememberSaveable(changelog) { mutableStateOf(false) }
    // Кнопка нужна, только если текст действительно не влез в свёрнутый вид.
    var overflows by remember(changelog) { mutableStateOf(false) }

    Column(modifier) {
        LedgerCard(highlighted = true) {
            Column(Modifier.padding(16.dp)) {
                LedgerSectionLabel("Что нового")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = changelog.trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.ink,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                    modifier = Modifier.animateContentSize(),
                )
                if (overflows || expanded) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            if (expanded) "Свернуть" else "Показать полностью",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppTheme.colors.accent,
                        )
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                }
                Text(meta, style = MonoMeta, color = AppTheme.colors.muted)
            }
        }
    }
}
