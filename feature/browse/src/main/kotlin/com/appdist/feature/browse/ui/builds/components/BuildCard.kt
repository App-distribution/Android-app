package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoAction
import com.appdist.core.ui.theme.MonoMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Строка истории релизов: версия и номер сборки слева, сжатые метаданные
 * справа, отдельный 48dp-таргет открытия. Содержимое не касается краёв карточки.
 */
@Composable
fun BuildCard(
    build: BuildUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VolcanicCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        stroke = AppTheme.colors.panelStrokeStrong,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = build.versionName.withVersionPrefix(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "build ${build.versionCode}",
                    style = MonoMeta,
                    color = AppTheme.colors.inkSoft,
                )
            }
            Text(
                text = build.compactMeta(),
                style = MonoAction,
                color = AppTheme.colors.inkSoft,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(.8f),
            )
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                when (build.installStatus) {
                    is InstallStatus.Installed -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Установлено",
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                    else -> Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** Дата загрузки сборки в моно-ритме макета. */
@Composable
fun rememberUploadDate(uploadDate: Long, pattern: String = "dd MMM yyyy"): String = remember(uploadDate) {
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(uploadDate))
}

/**
 * Две явные строки, а не свободный перенос: канал со стендом читаются вместе,
 * ветка — отдельно. Иначе длинное окружение съедает ветку многоточием.
 */
internal fun BuildUi.compactMeta(): String {
    val head = listOf(channel.take(3).uppercase(), environment.uppercase())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val branchLine = branch?.takeIf { it.isNotBlank() }?.let { "BR:${it.take(9)}" }
    return listOfNotNull(head.takeIf { it.isNotBlank() }, branchLine).joinToString("\n")
}

internal fun String.withVersionPrefix(): String = if (startsWith("v")) this else "v$this"
