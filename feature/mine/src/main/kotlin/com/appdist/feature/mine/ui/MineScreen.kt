package com.appdist.feature.mine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    onBuildClick: (String) -> Unit,
    viewModel: MineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Удалить все скачанные файлы?") },
            text = { Text("Освободится ${formatSize(state.downloadedTotalBytes)}. Сборки можно будет скачать заново.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(MineAction.DeleteAllDownloads)
                    showClearAllDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Мои сборки") }) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Active downloads section
            if (state.activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        "Загрузки",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(state.activeDownloads, key = { it.buildId }) { download ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(download.buildId, style = MaterialTheme.typography.bodyMedium)
                            val stateLabel = when (download.state) {
                                "downloading" -> "Загрузка…"
                                "verifying" -> "Проверка…"
                                "failed" -> "Ошибка"
                                else -> download.state
                            }
                            Text(
                                stateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { download.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (download.totalBytes > 0) {
                                Text(
                                    "${download.bytesLoaded / 1024 / 1024} / ${download.totalBytes / 1024 / 1024} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Downloaded files section
            if (state.downloaded.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Скачанные · ${formatSize(state.downloadedTotalBytes)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text("Очистить всё")
                        }
                    }
                }
                items(state.downloaded, key = { it.buildId }) { apk ->
                    val dateStr = remember(apk.completedAt) { dateFormatter.format(Date(apk.completedAt)) }
                    ElevatedCard(
                        onClick = { onBuildClick(apk.buildId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
                        ) {
                            Column(
                                Modifier.weight(1f).padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(apk.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${apk.channel} · ${formatSize(apk.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.onAction(MineAction.DeleteDownload(apk.buildId)) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить файл")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Install history section
            item {
                Text(
                    "История установок",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (state.installHistory.isEmpty()) {
                item {
                    Text(
                        "Нет установленных сборок",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.installHistory, key = { it.id }) { entry ->
                    val dateStr = remember(entry.installedAt) {
                        dateFormatter.format(Date(entry.installedAt))
                    }
                    ElevatedCard(
                        onClick = { onBuildClick(entry.buildId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.packageName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "v${entry.versionName} (${entry.versionCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "${bytes / 1024} КБ"
    else -> "$bytes Б"
}
