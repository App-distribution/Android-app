package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BuildCard(
    build: BuildUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = rememberUploadDate(build.uploadDate, "d MMM")
    val divider = AppTheme.colors.listDivider
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = divider,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - stroke / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke,
                )
            }
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                build.versionName.withVersionPrefix(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = AppTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "build ${build.versionCode}",
                style = MonoMeta.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
                color = AppTheme.colors.muted,
            )
        }
        Text(
            build.compactHistoryMeta(date),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
            color = AppTheme.colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 132.dp)
                .width(172.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTheme.colors.inkSoft,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
                .size(24.dp),
        )
    }
}

@Composable
fun rememberUploadDate(uploadDate: Long, pattern: String = "d MMM yyyy"): String = remember(uploadDate, pattern) {
    SimpleDateFormat(pattern, Locale("ru")).format(Date(uploadDate)).replace(".", "")
}

private fun BuildUi.compactHistoryMeta(date: String): String {
    return listOf(tagLabel, date)
        .filter { it.isNotBlank() }
        .joinToString("  •  ")
}

internal fun String.withVersionPrefix(): String = if (startsWith("v")) this else "v$this"
