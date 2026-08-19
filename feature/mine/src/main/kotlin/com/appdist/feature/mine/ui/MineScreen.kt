package com.appdist.feature.mine.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.GhostNumeral
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.RailAction
import com.appdist.core.ui.components.RailIconTarget
import com.appdist.core.ui.components.RailLabel
import com.appdist.core.ui.components.RailValue
import com.appdist.core.ui.components.RouteSwitcher
import com.appdist.core.ui.components.RouteTab
import com.appdist.core.ui.components.StorageBar
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.components.bottomGapAfterInsets
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.core.ui.theme.VolcanicAccent
import com.appdist.core.ui.theme.VolcanicAction
import com.appdist.core.ui.theme.VolcanicInk
import com.appdist.core.ui.theme.VolcanicMuted
import com.appdist.core.ui.theme.VolcanicPanelStrokeStrong
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MineScreen(
    onBuildClick: (String) -> Unit,
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onJournalsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: MineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy  ·  HH:mm", Locale.getDefault()) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val newest = state.downloaded.firstOrNull()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MineEffect.LaunchInstaller -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                is MineEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Удалить все скачанные файлы?") },
            text = { Text("Освободится ${formatSize(state.downloadedTotalBytes)}. Сборки можно скачать заново.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(MineAction.DeleteAllDownloads)
                    showClearAllDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Отмена") }
            },
        )
    }

    VolcanicScaffold(
        side = actionZoneSide,
        destination = GraphDestination.Downloads,
        zoneTop = {
            RailIconTarget(Icons.Default.Settings, "Настройки", onSettingsClick)
            Spacer(Modifier.height(22.dp))
            RailLabel("LOCAL")
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.downloaded.size.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                color = VolcanicInk,
            )
            Spacer(Modifier.height(12.dp))
            RailValue("${state.downloadedTotalBytes / 1024 / 1024}\nMB")
        },
        zoneAction = {
            ActionCaption("ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            RailAction(
                icon = Icons.Default.DownloadDone,
                label = "Установить",
                enabled = newest != null,
                onClick = { newest?.let { viewModel.onAction(MineAction.InstallDownload(it.buildId)) } },
                minHeight = 142.dp,
            )
        },
    ) {
        GhostNumeral(
            text = state.downloaded.size.toString().padStart(2, '0'),
            fontSize = 152.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 10.dp, bottom = 24.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 26.dp, bottom = bottomGapAfterInsets(margin = 28.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            item {
                Column {
                    Text("Скачанные", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.downloaded.size} APK  ·  ${formatSize(state.downloadedTotalBytes)}",
                        style = MonoLabel,
                        color = VolcanicMuted,
                    )
                    RouteSwitcher(
                        tabs = listOf(
                            RouteTab("Сборки", selected = false, onClick = onJournalsClick),
                            RouteTab("Скачанные", selected = true, onClick = {}),
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
            }

            if (state.activeDownloads.isNotEmpty()) {
                item { SectionLabel("ЗАГРУЖАЮТСЯ") }
                items(state.activeDownloads, key = { it.buildId }) { download ->
                    VolcanicCard(cornerRadius = 18.dp) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    download.buildId.take(12),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(download.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VolcanicAccent,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { download.progress },
                                color = VolcanicAccent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                            )
                            if (download.totalBytes > 0) {
                                Text(
                                    "${formatSize(download.bytesLoaded)} / ${formatSize(download.totalBytes)}",
                                    style = MonoMeta,
                                    color = VolcanicMuted,
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(state.downloaded) { index, apk ->
                DownloadedEntry(
                    apk = apk,
                    featured = index == 0,
                    completedAt = remember(apk.completedAt) { dateFormatter.format(Date(apk.completedAt)) },
                    onOpen = { onBuildClick(apk.buildId) },
                    onInstall = { viewModel.onAction(MineAction.InstallDownload(apk.buildId)) },
                    onDelete = { viewModel.onAction(MineAction.DeleteDownload(apk.buildId)) },
                )
            }

            if (state.downloaded.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = 8.dp)) {
                        StorageBar(state.storageFraction)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ЛОКАЛЬНО  ·  ${formatSize(state.downloadedTotalBytes)}",
                                style = MonoLabel,
                                color = VolcanicMuted,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { showClearAllDialog = true }) { Text("Очистить") }
                        }
                    }
                }
            }

            item { SectionLabel("ИСТОРИЯ УСТАНОВОК", Modifier.padding(top = 8.dp)) }
            if (state.installHistory.isEmpty()) {
                item {
                    VolcanicCard(cornerRadius = 18.dp) {
                        Text(
                            "Пока нет установленных сборок",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VolcanicMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(state.installHistory, key = { it.id }) { entry ->
                    val dateStr = remember(entry.installedAt) { dateFormatter.format(Date(entry.installedAt)) }
                    VolcanicCard(
                        cornerRadius = 18.dp,
                        stroke = VolcanicPanelStrokeStrong,
                        onClick = { onBuildClick(entry.buildId) },
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "v${entry.versionName} (${entry.versionCode})",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(entry.packageName, style = MonoMeta, color = VolcanicMuted, maxLines = 1)
                            Text(dateStr.uppercase(), style = MonoLabel, color = VolcanicAccent)
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Свежайший скачанный APK получает крупную подачу: версия, метаданные и время
 * загрузки читаются с расстояния. Остальные — компактной строкой.
 */
@Composable
private fun DownloadedEntry(
    apk: DownloadedApkUi,
    featured: Boolean,
    completedAt: String,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    VolcanicCard(
        cornerRadius = if (featured) 20.dp else 18.dp,
        onClick = onOpen,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)) {
            if (apk.channel.isNotBlank()) {
                Text(apk.channel.uppercase(), style = MonoLabel, color = VolcanicMuted)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = apk.versionName.takeIf { it.isNotBlank() }?.let { "v$it" } ?: apk.title,
                style = if (featured) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "BUILD ${apk.versionCode}  ·  ${formatSize(apk.sizeBytes)}",
                style = MonoMeta,
                color = VolcanicMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(completedAt.uppercase(), style = MonoLabel, color = VolcanicAccent)
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить файл",
                        tint = VolcanicMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .background(VolcanicInk, RoundedCornerShape(16.dp))
                        .clickable(onClick = onInstall),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Установить",
                        tint = VolcanicAction,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MonoLabel, color = VolcanicAccent, modifier = modifier)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "${bytes / 1024} КБ"
    else -> "$bytes Б"
}
