package com.appdist.feature.browse.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LedgerBadge
import com.appdist.core.ui.components.LedgerCard
import com.appdist.core.ui.components.LedgerIconButton
import com.appdist.core.ui.components.LedgerPrimaryButton
import com.appdist.core.ui.components.LedgerScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta

@Composable
fun ProjectsScreen(
    onProjectClick: (ProjectResponse) -> Unit,
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    val projects = remember(state.projects, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) state.projects else state.projects.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    LedgerScreen {
        when {
            state.isLoading && state.projects.isEmpty() -> LoadingScreen(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
            state.error != null && state.projects.isEmpty() -> ErrorScreen(
                error = state.error!!,
                title = "Не удалось загрузить приложения",
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ProductHeader(onSettingsClick)
                    Text(
                        "${state.projects.size} приложений доступно",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                    )
                    ProjectSearch(value = searchText, onValueChange = { searchText = it })
                    Spacer(Modifier.height(10.dp))
                }
                if (projects.isEmpty()) {
                    item {
                        EmptyScreen(
                            if (state.projects.isEmpty()) "Приложений пока нет" else "Ничего не найдено",
                            Modifier.height(240.dp),
                        )
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectEntry(
                            project = project,
                            latest = state.latestBuilds[project.id],
                            latestBuildLoading = state.isLatestBuildsLoading,
                            latestBuildError = state.latestBuildsError != null,
                            onClick = { onProjectClick(project) },
                        )
                    }
                }
                item {
                    DownloadsRow(onClick = onDownloadsClick)
                    Text(
                        "Выберите приложение, чтобы открыть журнал сборок.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Приложения",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.weight(1f),
        )
        LedgerIconButton(Icons.Default.Settings, "Настройки", onSettingsClick)
    }
}

@Composable
private fun ProjectSearch(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AppTheme.colors.field, shape)
            .border(1.dp, AppTheme.colors.panelStroke, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = AppTheme.colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Найти приложение", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.ink),
                cursorBrush = SolidColor(AppTheme.colors.ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProjectEntry(
    project: ProjectResponse,
    latest: BuildEntity?,
    latestBuildLoading: Boolean,
    latestBuildError: Boolean,
    onClick: () -> Unit,
) {
    LedgerCard(highlighted = true, onClick = onClick) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTheme.colors.actionTile, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = AppTheme.colors.onActionTile,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        project.packageName,
                        style = MonoMeta,
                        color = AppTheme.colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (project.packageName.contains("test", ignoreCase = true)) LedgerBadge("test")
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.panelStroke))
            Spacer(Modifier.height(16.dp))

            if (latest != null) {
                Text("Последняя сборка", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.accent)
                Text(
                    latest.versionName.withVersionPrefix(),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    listOf("build ${latest.versionCode}", latest.environment, latest.channel)
                        .filter { it.isNotBlank() }
                        .joinToString("  ·  "),
                    style = MonoMeta,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                )
            } else {
                val (title, message) = when {
                    latestBuildLoading -> "Проверяем сборки" to
                        "Загружаем последнюю доступную версию."
                    latestBuildError -> "Сборки не обновлены" to
                        "Откройте журнал или повторите позже."
                    else -> "Сборок пока нет" to
                        "Для проекта пока нет доступных версий."
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                )
            }

            LedgerPrimaryButton(
                label = "Открыть журнал",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun DownloadsRow(onClick: () -> Unit) {
    LedgerCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = AppTheme.colors.inkSoft,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Скачанные",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AppTheme.colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun String.withVersionPrefix(): String = if (startsWith("v")) this else "v$this"
