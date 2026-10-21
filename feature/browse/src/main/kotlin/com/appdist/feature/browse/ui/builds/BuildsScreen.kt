package com.appdist.feature.browse.ui.builds

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appdist.core.common.AppError
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.common.toAppError
import com.appdist.core.downloads.DownloadState
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LedgerCard
import com.appdist.core.ui.components.LedgerPrimaryButton
import com.appdist.core.ui.components.LedgerScreen
import com.appdist.core.ui.components.LedgerTopBar
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoMeta
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
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val latestInstallState by viewModel.latestInstallState.collectAsStateWithLifecycle()
    val builds = viewModel.builds.collectAsLazyPagingItems()
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var signatureMismatch by remember { mutableStateOf<String?>(null) }
    var downgradeWarning by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BuildsEffect.LaunchInstaller -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                is BuildsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is BuildsEffect.ShowSignatureMismatchDialog -> signatureMismatch = effect.installedFp
                is BuildsEffect.ShowDowngradeWarning -> {
                    downgradeWarning = effect.installedCode to effect.newCode
                }
            }
        }
    }

    LedgerScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            LedgerTopBar(
                title = projectName.ifBlank { "Журнал приложения" },
                leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                leadingDescription = "Назад",
                onLeadingClick = onBack,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Сборки", style = MaterialTheme.typography.displaySmall)
                if (packageName.isNotBlank()) {
                    Text(
                        packageName,
                        style = MonoMeta,
                        color = AppTheme.colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
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
                    options = filterOptions,
                    onFlavorSelected = viewModel::setFlavor,
                    onStandSelected = viewModel::setStand,
                )
            }

            when {
                builds.loadState.refresh is LoadState.Loading -> LoadingScreen(Modifier.weight(1f))
                builds.loadState.refresh is LoadState.Error -> {
                    val appError = (builds.loadState.refresh as LoadState.Error).error.toAppError()
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
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                ) {
                    item {
                        builds[0]?.let { build ->
                            LaunchedEffect(build.id, projectName, packageName) {
                                viewModel.setLatestBuild(build, projectName, packageName)
                            }
                            val actionableBuild = latestInstallState.build
                                ?.takeIf { it.id == build.id }
                                ?: build.copy(projectName = projectName, packageName = packageName)
                            val downloadState = if (latestInstallState.build?.id == build.id) {
                                latestInstallState.downloadState
                            } else {
                                DownloadState.Idle
                            }
                            LatestBuildCard(
                                build = actionableBuild,
                                downloadState = downloadState,
                                onOpen = { onBuildClick(build.id) },
                                onInstall = viewModel::onLatestInstallClick,
                            )
                        }
                    }
                    if (builds.itemCount > 1) {
                        item {
                            Text(
                                "Предыдущие",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = AppTheme.colors.ink,
                                modifier = Modifier.padding(top = 26.dp, bottom = 16.dp),
                            )
                        }
                        items(builds.itemCount - 1) { historyIndex ->
                            builds[historyIndex + 1]?.let { build ->
                                BuildCard(build = build, onClick = { onBuildClick(build.id) })
                            }
                        }
                    }
                    if (builds.loadState.append is LoadState.Loading) {
                        item { LoadingScreen(Modifier.height(72.dp)) }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    signatureMismatch?.let { fingerprint ->
        BlockingInstallDialog(
            title = "Подпись приложения отличается",
            message = "Установленная версия подписана другим сертификатом (" +
                fingerprint.take(12) + "…). Удалите её в настройках Android, затем повторите установку.",
            onDismiss = { signatureMismatch = null },
            onOpenSettings = {
                signatureMismatch = null
                latestInstallState.build?.packageName?.let { context.openAppSettings(it) }
            },
        )
    }
    downgradeWarning?.let { warning ->
        BlockingInstallDialog(
            title = "Нужен откат версии",
            message = "На устройстве установлена сборка #" + warning.first +
                ", а выбрана #" + warning.second +
                ". Удалите текущую версию в настройках Android и вернитесь к установке.",
            onDismiss = { downgradeWarning = null },
            onOpenSettings = {
                downgradeWarning = null
                latestInstallState.build?.packageName?.let { context.openAppSettings(it) }
            },
        )
    }
}

@Composable
private fun LatestBuildCard(
    build: BuildUi,
    downloadState: DownloadState,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
) {
    val date = rememberUploadDate(build.uploadDate)
    LedgerCard(
        highlighted = true,
        onClick = onOpen,
        modifier = Modifier.height(236.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "Последняя сборка",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = AppTheme.colors.highlightInk,
                modifier = Modifier.offset(x = 18.dp, y = 18.dp),
            )
            BuildEnvironmentBadge(
                label = build.tagLabel.ifBlank { "QA" },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-18).dp, y = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 56.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    build.versionName.withVersionPrefix(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 40.sp,
                        lineHeight = 48.sp,
                        letterSpacing = (-1.4).sp,
                    ),
                    color = AppTheme.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "build ${build.versionCode}",
                    style = MonoMeta.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = AppTheme.colors.inkSoft,
                )
            }
            build.changelog?.takeIf { it.isNotBlank() }?.let { changelog ->
                Text(
                    changelog,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = 18.dp, y = 112.dp)
                        .padding(end = 36.dp),
                )
            }
            Text(
                listOfNotNull(
                    date,
                    "${build.fileSize / 1024 / 1024} MB",
                    build.channel.takeIf { it.isNotBlank() }?.lowercase(),
                ).joinToString("  •  "),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = 18.dp, y = 138.dp)
                    .padding(end = 36.dp),
            )
            LatestBuildInstallAction(
                build = build,
                downloadState = downloadState,
                onClick = onInstall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun LatestBuildInstallAction(
    build: BuildUi,
    downloadState: DownloadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloading = downloadState as? DownloadState.Downloading
    if (downloading != null) {
        val progress = downloading.progress.coerceIn(0f, 1f)
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(AppTheme.colors.surface)
                .border(1.dp, AppTheme.colors.panelStrokeStrong, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(AppTheme.colors.progress),
            )
            Text(
                (if (downloading.waiting) "Ожидание сети · " else "Загрузка · ") + "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.ink,
            )
        }
        return
    }

    val enabled = when {
        downloadState is DownloadState.Verifying -> false
        build.installStatus is InstallStatus.Installed &&
            downloadState !is DownloadState.ReadyToInstall -> false
        build.installStatus is InstallStatus.Incompatible -> false
        else -> true
    }
    val label = when {
        downloadState is DownloadState.Verifying -> "Проверка"
        downloadState is DownloadState.ReadyToInstall -> "Установить"
        downloadState is DownloadState.Failed -> "Повторить"
        build.installStatus is InstallStatus.UpdateAvailable -> "Обновить"
        build.installStatus is InstallStatus.Installed -> "Установлено"
        build.installStatus is InstallStatus.InstalledNewer -> "Откатить"
        build.installStatus is InstallStatus.Incompatible -> "Недоступно"
        else -> "Установить"
    }
    val icon = when {
        downloadState is DownloadState.ReadyToInstall -> Icons.Default.InstallMobile
        build.installStatus is InstallStatus.Installed -> Icons.Default.CheckCircle
        else -> Icons.Default.Download
    }
    LedgerPrimaryButton(
        label = label,
        icon = icon,
        enabled = enabled,
        height = 48.dp,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun BlockingInstallDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun android.content.Context.openAppSettings(packageName: String) {
    startActivity(
        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    )
}

@Composable
private fun BuildEnvironmentBadge(label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .height(28.dp)
            .widthIn(min = 40.dp)
            .background(AppTheme.colors.surface, shape)
            .border(1.dp, AppTheme.colors.badgeStroke, shape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            style = MonoMeta.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = AppTheme.colors.highlightInk,
            maxLines = 1,
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AppTheme.colors.field, shape)
            .border(1.dp, AppTheme.colors.panelStroke, shape)
            .padding(horizontal = 14.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = AppTheme.colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Найти сборку", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
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
