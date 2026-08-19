package com.appdist.feature.browse.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.GhostNumeral
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.components.PanelDivider
import com.appdist.core.ui.components.RailAction
import com.appdist.core.ui.components.RailIconTarget
import com.appdist.core.ui.components.RouteSwitcher
import com.appdist.core.ui.components.RouteTab
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.components.bottomGapAfterInsets
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.core.ui.theme.VolcanicAccent
import com.appdist.core.ui.theme.VolcanicMuted
import com.appdist.core.ui.theme.VolcanicPanelStroke

@Composable
fun ProjectsScreen(
    onProjectClick: (ProjectResponse) -> Unit,
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    VolcanicScaffold(
        side = actionZoneSide,
        destination = GraphDestination.Journals,
        zoneTop = {
            RailIconTarget(Icons.Default.Settings, "Настройки", onSettingsClick)
        },
        // Журнал открывается тапом по карточке, поэтому единственное действие
        // зоны здесь — переход к скачанным сборкам.
        zoneAction = {
            ActionCaption("ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            RailAction(
                icon = Icons.Default.Download,
                label = "Скачанные",
                onClick = onDownloadsClick,
                minHeight = 126.dp,
            )
        },
    ) {
        GhostNumeral(
            text = journalCode(state.projects.size - 1),
            fontSize = 152.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 10.dp, bottom = 24.dp),
        )

        val header: @Composable () -> Unit = {
            ProjectsHeader(
                count = state.projects.size,
                onDownloadsClick = onDownloadsClick,
            )
        }

        when {
            state.isLoading && state.projects.isEmpty() -> LoadingScreen(
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            )
            state.error != null && state.projects.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 16.dp),
            ) {
                header()
                ErrorScreen(
                    error = state.error!!,
                    title = "Не удалось загрузить журналы",
                    onRetry = viewModel::retry,
                    modifier = Modifier.weight(1f),
                )
            }
            state.projects.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 16.dp),
            ) {
                header()
                EmptyScreen("Журналов сборок пока нет", Modifier.weight(1f))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 26.dp, bottom = bottomGapAfterInsets(margin = 28.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                item { header() }
                items(state.projects, key = { it.id }) { project ->
                    ProjectJournal(
                        project = project,
                        latest = state.latestBuilds[project.id],
                        onClick = { onProjectClick(project) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsHeader(count: Int, onDownloadsClick: () -> Unit) {
    Column {
        Text("Приложения", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ДОСТУПНО  ·  ${journalCode(count - 1)}",
            style = MonoLabel,
            color = VolcanicMuted,
        )
        RouteSwitcher(
            tabs = listOf(
                RouteTab("Сборки", selected = true, onClick = {}),
                RouteTab("Скачанные", selected = false, onClick = onDownloadsClick),
            ),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

/** Карточка — сама точка входа в журнал: тап открывает историю релизов. */
@Composable
private fun ProjectJournal(
    project: ProjectResponse,
    latest: BuildEntity?,
    onClick: () -> Unit,
) {
    VolcanicCard(
        cornerRadius = 20.dp,
        stroke = VolcanicPanelStroke,
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = VolcanicAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(project.packageName, style = MonoMeta, color = VolcanicMuted, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            PanelDivider()
            Spacer(Modifier.height(16.dp))
            if (latest != null) {
                Text("LATEST BUILD", style = MonoLabel, color = VolcanicAccent)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.versionName.withVersionPrefix(),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOf(
                        "${latest.versionCode}",
                        latest.environment,
                        latest.channel,
                    ).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MonoMeta,
                    color = VolcanicMuted,
                )
            } else {
                Text("ЖУРНАЛ ПУСТ", style = MonoLabel, color = VolcanicMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Откройте журнал, чтобы загрузить историю релизов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VolcanicMuted,
                )
            }
        }
    }
}

/** Порядковый код журнала в стиле макета: 01, 02, … */
private fun journalCode(zeroBasedIndex: Int): String =
    (zeroBasedIndex + 1).coerceAtLeast(0).toString().padStart(2, '0')

private fun String.withVersionPrefix(): String = if (startsWith("v")) this else "v$this"
