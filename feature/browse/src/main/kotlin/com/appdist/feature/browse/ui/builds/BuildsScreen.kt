package com.appdist.feature.browse.ui.builds

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appdist.core.common.AppError
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.toAppError
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.GhostLabel
import com.appdist.core.ui.components.GhostNumeral
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.components.RailAction
import com.appdist.core.ui.components.RailDivider
import com.appdist.core.ui.components.RailDot
import com.appdist.core.ui.components.RailIconTarget
import com.appdist.core.ui.components.RailLabel
import com.appdist.core.ui.components.RailValue
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.components.bottomGapAfterInsets
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.core.ui.theme.MonoValue
import com.appdist.core.ui.theme.VolcanicField
import com.appdist.core.ui.theme.VolcanicGhostLabel
import com.appdist.core.ui.theme.VolcanicInk
import com.appdist.core.ui.theme.VolcanicInkSoft
import com.appdist.core.ui.theme.VolcanicMuted
import com.appdist.core.ui.theme.VolcanicMutedDim
import com.appdist.core.ui.theme.VolcanicPanelStroke
import com.appdist.feature.browse.ui.builds.components.BuildCard
import com.appdist.feature.browse.ui.builds.components.FilterChipsRow
import com.appdist.feature.browse.ui.builds.components.rememberUploadDate
import com.appdist.feature.browse.ui.builds.components.withVersionPrefix

@Composable
fun BuildsScreen(
    projectId: String,
    projectName: String,
    packageName: String,
    onBack: () -> Unit,
    onBuildClick: (String) -> Unit,
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: BuildsViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val builds = viewModel.builds.collectAsLazyPagingItems()
    var searchText by remember { mutableStateOf("") }
    val latest = builds.itemSnapshotList.items.firstOrNull()

    VolcanicScaffold(
        side = actionZoneSide,
        destination = GraphDestination.Builds,
        zoneTop = {
            RailIconTarget(Icons.Default.Settings, "Настройки", onSettingsClick)
            if (latest != null) {
                Spacer(Modifier.height(22.dp))
                RailLabel("LATEST")
                Spacer(Modifier.height(8.dp))
                RailDot()
                Spacer(Modifier.height(16.dp))
                RailLabel(latest.channel.uppercase())
                Spacer(Modifier.height(10.dp))
                RailDivider()
                Spacer(Modifier.height(10.dp))
                RailLabel(latest.environment.uppercase())
                Spacer(Modifier.height(14.dp))
                RailValue("${latest.fileSize / 1024 / 1024}\nMB")
            }
        },
        zoneAction = {
            ActionCaption("ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            RailAction(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Открыть",
                enabled = latest != null,
                onClick = { latest?.let { onBuildClick(it.id) } },
            )
        },
    ) {
        if (latest != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = 20.dp),
            ) {
                GhostNumeral("${latest.versionCode}")
                GhostLabel(
                    "BUILD ${latest.versionCode}",
                    color = VolcanicGhostLabel,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
                latest.branch?.takeIf { it.isNotBlank() }?.let { branch ->
                    GhostLabel(
                        "BR:$branch / APK",
                        color = VolcanicGhostLabel,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            BuildsTopBar(projectName = projectName, onBack = onBack)

            Column(Modifier.padding(start = 20.dp, end = 16.dp)) {
                Text("Сборки", style = MaterialTheme.typography.displaySmall)
                if (packageName.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(packageName, style = MonoMeta, color = VolcanicInkSoft)
                }
                Spacer(Modifier.height(14.dp))
                SearchField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.setSearch(it)
                    },
                )
                Spacer(Modifier.height(10.dp))
                FilterChipsRow(
                    filters = filters,
                    onChannelSelected = viewModel::setChannel,
                    onEnvironmentSelected = viewModel::setEnvironment,
                )
            }

            when {
                builds.loadState.refresh is LoadState.Loading -> LoadingScreen(Modifier.weight(1f))
                builds.loadState.refresh is LoadState.Error -> {
                    val error = (builds.loadState.refresh as LoadState.Error).error
                    val appError = error.toAppError()
                    ErrorScreen(
                        error = appError,
                        title = "Не удалось загрузить сборки",
                        description = if (appError is AppError.Network) {
                            "Сервис временно недоступен. Поиск и фильтры сохранены."
                        } else null,
                        onRetry = { builds.refresh() },
                        modifier = Modifier.weight(1f),
                    )
                }
                builds.itemCount == 0 -> EmptyScreen("Сборок не найдено", Modifier.weight(1f))
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 20.dp, bottom = bottomGapAfterInsets(margin = 28.dp)),
                    modifier = Modifier.weight(1f),
                ) {
                    items(builds.itemCount) { index ->
                        builds[index]?.let { build ->
                            when (index) {
                                0 -> LatestBuildHero(
                                    build = build,
                                    onClick = { onBuildClick(build.id) },
                                )
                                1 -> Text(
                                    "Предыдущие",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 30.dp, bottom = 14.dp),
                                )
                                else -> Unit
                            }
                            if (index > 0) {
                                BuildCard(
                                    build = build,
                                    onClick = { onBuildClick(build.id) },
                                    modifier = Modifier.padding(bottom = 10.dp),
                                )
                            }
                        }
                    }
                    if (builds.loadState.append is LoadState.Loading) {
                        item { LoadingScreen(Modifier.height(76.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildsTopBar(projectName: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        Text(
            text = projectName.ifBlank { "Журнал приложения" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Новейшая сборка получает крупную подачу прямо на грунте: версия, номер и
 * сжатые метаданные видны до открытия деталей.
 */
@Composable
private fun LatestBuildHero(build: BuildUi, onClick: () -> Unit) {
    val date = rememberUploadDate(build.uploadDate)
    Column(Modifier.clickable(onClick = onClick)) {
        Text(
            text = build.versionName.withVersionPrefix(),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text("build ${build.versionCode}", style = MonoValue, color = VolcanicInk)
        Spacer(Modifier.height(10.dp))
        build.changelog?.takeIf { it.isNotBlank() }?.let { changelog ->
            Text(
                text = changelog,
                style = MaterialTheme.typography.bodyMedium,
                color = VolcanicInkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = listOfNotNull(
                date.uppercase(),
                "${build.fileSize / 1024 / 1024} MB",
                build.branch?.takeIf { it.isNotBlank() }?.let { "BR:$it" },
            ).joinToString("  ·  "),
            style = MonoMeta,
            color = VolcanicInkSoft,
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(VolcanicField, shape)
            .border(1.dp, VolcanicPanelStroke, shape)
            .padding(horizontal = 12.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    "Версия или ветка",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VolcanicMutedDim,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = VolcanicInk),
                cursorBrush = SolidColor(com.appdist.core.ui.theme.VolcanicAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
