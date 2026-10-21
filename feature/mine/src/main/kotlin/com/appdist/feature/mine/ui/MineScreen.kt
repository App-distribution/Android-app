package com.appdist.feature.mine.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.LedgerCard
import com.appdist.core.ui.components.LedgerPrimaryButton
import com.appdist.core.ui.components.LedgerScreen
import com.appdist.core.ui.components.LedgerSectionLabel
import com.appdist.core.ui.components.LedgerTopBar
import com.appdist.core.ui.components.StorageBar
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta
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
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MineEffect.LaunchInstaller -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                is MineEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Удалить все сборки?") },
            text = {
                Text(
                    "Освободится " + formatSize(state.downloadedTotalBytes) +
                        ". Сборки можно скачать заново."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(MineAction.DeleteAllDownloads)
                        showClearAllDialog = false
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Отмена") }
            },
        )
    }

    LedgerScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LedgerTopBar(
                    title = "",
                    leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leadingDescription = "К приложениям",
                    onLeadingClick = onJournalsClick,
                    trailingIcon = Icons.Default.Settings,
                    trailingDescription = "Настройки",
                    onTrailingClick = onSettingsClick,
                )
                Text("Скачанные", style = MaterialTheme.typography.displaySmall)
                Text(
                    state.downloaded.size.toString() + " APK  ·  " +
                        formatSize(state.downloadedTotalBytes),
                    style = MonoMeta,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }

            if (state.activeDownloads.isNotEmpty()) {
                item { LedgerSectionLabel("Загружаются") }
                items(state.activeDownloads, key = { it.buildId }) { download ->
                    ActiveDownloadCard(
                        title = download.buildId.take(12),
                        progress = download.progress,
                        loaded = download.bytesLoaded,
                        total = download.totalBytes,
                    )
                }
            }

            if (state.downloaded.isEmpty()) {
                item {
                    LedgerCard {
                        Column(Modifier.padding(18.dp)) {
                            Text("Скачанных сборок пока нет", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Установленные APK появятся здесь.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.muted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(state.downloaded, key = { _, apk -> apk.buildId }) { index, apk ->
                    DownloadedEntry(
                        apk = apk,
                        featured = index == 0,
                        completedAt = remember(apk.completedAt) {
                            dateFormatter.format(Date(apk.completedAt))
                        },
                        onOpen = { onBuildClick(apk.buildId) },
                        onInstall = { viewModel.onAction(MineAction.InstallDownload(apk.buildId)) },
                        onDelete = { viewModel.onAction(MineAction.DeleteDownload(apk.buildId)) },
                    )
                }
                item {
                    StorageSummary(
                        usedBytes = state.downloadedTotalBytes,
                        fraction = state.storageFraction,
                    )
                }
                item {
                    DeleteAllButton(
                        size = formatSize(state.downloadedTotalBytes),
                        onClick = { showClearAllDialog = true },
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ActiveDownloadCard(title: String, progress: Float, loaded: Long, total: Long) {
    LedgerCard(highlighted = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    (progress * 100).toInt().toString() + "%",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(AppTheme.colors.track, RoundedCornerShape(4.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(AppTheme.colors.progress, RoundedCornerShape(4.dp)),
                )
            }
            if (total > 0) {
                Text(
                    formatSize(loaded) + " / " + formatSize(total),
                    style = MonoMeta,
                    color = AppTheme.colors.muted,
                )
            }
        }
    }
}

@Composable
private fun DownloadedEntry(
    apk: DownloadedApkUi,
    featured: Boolean,
    completedAt: String,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    LedgerCard(highlighted = featured, onClick = onOpen) {
        Column(Modifier.padding(16.dp)) {
            if (featured) {
                LedgerSectionLabel("Последняя")
                Spacer(Modifier.height(8.dp))
            }
            Text(
                apk.versionName.takeIf { it.isNotBlank() }?.let { "v" + it } ?: apk.title,
                style = if (featured) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "BUILD " + apk.versionCode +
                    apk.channel.takeIf { it.isNotBlank() }?.let { "  ·  " + it.uppercase() }.orEmpty(),
                style = MonoMeta,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    completedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(formatSize(apk.sizeBytes), style = MonoMeta)
            }
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LedgerPrimaryButton(
                    label = "Установить",
                    icon = Icons.Default.DownloadDone,
                    onClick = onInstall,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppTheme.colors.surface, RoundedCornerShape(6.dp))
                        .border(1.dp, AppTheme.colors.panelStroke, RoundedCornerShape(6.dp))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить сборку",
                        tint = AppTheme.colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageSummary(usedBytes: Long, fraction: Float) {
    LedgerCard {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Хранилище приложения",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(formatSize(usedBytes), style = MonoMeta, color = AppTheme.colors.muted)
            }
            StorageBar(fraction, Modifier.padding(top = 16.dp))
            Text(
                "Удалите старые APK, чтобы освободить место.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun DeleteAllButton(size: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(AppTheme.colors.surfaceRaised, shape)
            .border(1.dp, AppTheme.colors.panelStrokeStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.DeleteSweep,
            contentDescription = null,
            tint = AppTheme.colors.inkSoft,
            modifier = Modifier.size(20.dp),
        )
        Text(
            "Удалить все сборки",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        Text(size, style = MonoMeta, color = AppTheme.colors.muted)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> (bytes / 1024).toString() + " КБ"
    else -> bytes.toString() + " Б"
}
