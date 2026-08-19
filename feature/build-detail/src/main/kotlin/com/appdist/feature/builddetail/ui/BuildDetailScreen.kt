package com.appdist.feature.builddetail.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.AppError
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadState
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.GhostNumeral
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.components.RailActionTile
import com.appdist.core.ui.components.RailDivider
import com.appdist.core.ui.components.RailDot
import com.appdist.core.ui.components.RailIconTarget
import com.appdist.core.ui.components.RailLabel
import com.appdist.core.ui.components.RailValue
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoAction
import com.appdist.core.ui.theme.MonoLabel
import com.appdist.core.ui.theme.MonoMeta
import com.appdist.feature.builddetail.ui.components.ChangelogSection
import com.appdist.feature.builddetail.ui.components.TechDetailsSection

@Composable
fun BuildDetailScreen(
    buildId: String,
    onBack: () -> Unit,
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onSettingsClick: () -> Unit = {},
    viewModel: BuildDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var signatureMismatch by remember { mutableStateOf<String?>(null) }
    var downgradeWarning by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BuildDetailEffect.LaunchInstaller -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                is BuildDetailEffect.CopyToClipboard -> {
                    clipboard.setText(AnnotatedString(effect.text))
                    snackbarHostState.showSnackbar("Ссылка скопирована")
                }
                is BuildDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is BuildDetailEffect.ShowSignatureMismatchDialog -> {
                    signatureMismatch = effect.installedFp
                }
                is BuildDetailEffect.ShowDowngradeWarning -> {
                    downgradeWarning = effect.installedCode to effect.newCode
                }
            }
        }
    }

    val build = state.build
    val downloading = state.downloadState as? DownloadState.Downloading

    VolcanicScaffold(
        side = actionZoneSide,
        destination = GraphDestination.BuildDetail,
        zoneTop = {
            RailIconTarget(Icons.Default.Settings, "Настройки", onSettingsClick)
            if (build != null) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = build.versionName.versionTail(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppTheme.colors.ink,
                )
                Spacer(Modifier.height(18.dp))
                if (build.isLatestInChannel) {
                    RailLabel("LATEST")
                    Spacer(Modifier.height(8.dp))
                    RailDot()
                    Spacer(Modifier.height(16.dp))
                }
                RailLabel(build.channel.uppercase())
                Spacer(Modifier.height(6.dp))
                RailLabel(build.environment.uppercase())
                Spacer(Modifier.height(12.dp))
                RailDivider()
                Spacer(Modifier.height(12.dp))
                RailValue("APK\n${build.fileSize / 1024 / 1024} MB")
            }
        },
        zoneAction = {
            ActionCaption(if (downloading != null) "ЗАГРУЗКА" else "ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            if (build != null) {
                InstallAction(
                    build = build,
                    downloadState = state.downloadState,
                    onAction = viewModel::onAction,
                )
            }
        },
    ) {
        val error = state.error
        when {
            state.isLoading -> LoadingScreen(
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            )
            error != null -> ErrorScreen(
                error = error,
                title = "Не удалось загрузить сборку",
                description = if (error is AppError.Network) {
                    "Сервер не вернул данные этой сборки. Попробуйте ещё раз."
                } else null,
                onRetry = { viewModel.onAction(BuildDetailAction.RetryClicked) },
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            )
            build != null -> {
                GhostNumeral(
                    text = "${build.versionCode}",
                    fontSize = 130.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 8.dp, bottom = 20.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BuildDetailTopBar(
                        projectName = build.projectName,
                        onBack = onBack,
                        onCopyLink = { viewModel.onAction(BuildDetailAction.CopyLinkClicked) },
                    )
                    Column(Modifier.padding(start = 20.dp, end = 16.dp)) {
                        Text(
                            text = build.versionName.versionHead(),
                            style = MaterialTheme.typography.displayMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = listOfNotNull(
                                "BUILD ${build.versionCode}",
                                build.branch?.takeIf { it.isNotBlank() }?.let { "BR:$it" },
                            ).joinToString("  ·  "),
                            style = MonoLabel,
                            color = AppTheme.colors.muted,
                        )
                        if (!build.changelog.isNullOrBlank()) {
                            ChangelogSection(
                                changelog = build.changelog.orEmpty(),
                                uploadDate = build.uploadDate,
                                fileSize = build.fileSize,
                                modifier = Modifier.padding(top = 26.dp),
                            )
                        }
                        TechDetailsSection(build = build, modifier = Modifier.padding(top = 18.dp))
                        Spacer(Modifier.height(34.dp))
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    signatureMismatch?.let { fingerprint ->
        BlockingInstallDialog(
            title = "Подпись приложения отличается",
            message = "Установленная версия подписана другим сертификатом (${fingerprint.take(12)}…). Удалите её в настройках Android, затем повторите установку.",
            onDismiss = { signatureMismatch = null },
            onOpenSettings = {
                signatureMismatch = null
                state.build?.packageName?.let { context.openAppSettings(it) }
            },
        )
    }
    downgradeWarning?.let { (installedCode, newCode) ->
        BlockingInstallDialog(
            title = "Нужен откат версии",
            message = "На устройстве установлена сборка #$installedCode, а выбрана #$newCode. Удалите текущую версию в настройках Android и вернитесь к установке.",
            onDismiss = { downgradeWarning = null },
            onOpenSettings = {
                downgradeWarning = null
                state.build?.packageName?.let { context.openAppSettings(it) }
            },
        )
    }
}

@Composable
private fun BuildDetailTopBar(
    projectName: String,
    onBack: () -> Unit,
    onCopyLink: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(start = 8.dp, end = 12.dp),
    ) {
        PanelIconTarget(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack,
        )
        Text(
            text = projectName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PanelIconTarget(
            icon = Icons.Default.Share,
            contentDescription = "Скопировать ссылку",
            onClick = onCopyLink,
        )
    }
}

@Composable
private fun PanelIconTarget(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(48.dp)
            .background(AppTheme.colors.panel, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
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

/**
 * Основное действие живёт в action-зоне и остаётся одной плиткой во всех
 * состояниях: скачать, прогресс с отменой, проверка, установка.
 */
@Composable
private fun InstallAction(
    build: BuildUi,
    downloadState: DownloadState,
    onAction: (BuildDetailAction) -> Unit,
) {
    val downloading = downloadState as? DownloadState.Downloading
    val enabled = when {
        downloadState is DownloadState.Verifying -> false
        build.installStatus is InstallStatus.Installed && downloadState !is DownloadState.ReadyToInstall -> false
        build.installStatus is InstallStatus.Incompatible -> false
        else -> true
    }
    val label = when {
        downloading != null -> "Отменить"
        downloadState is DownloadState.Verifying -> "Проверка"
        downloadState is DownloadState.ReadyToInstall -> "Установить"
        build.installStatus is InstallStatus.UpdateAvailable -> "Обновить"
        build.installStatus is InstallStatus.Installed -> "Готово"
        build.installStatus is InstallStatus.InstalledNewer -> "Откатить"
        build.installStatus is InstallStatus.Incompatible -> "Недоступно"
        else -> "Скачать\nи установить"
    }
    val icon = when {
        downloading != null -> Icons.Default.Cancel
        downloadState is DownloadState.ReadyToInstall -> Icons.Default.InstallMobile
        build.installStatus is InstallStatus.Installed -> Icons.Default.CheckCircle
        else -> Icons.Default.Download
    }

    RailActionTile(
        onClick = {
            if (downloading != null) onAction(BuildDetailAction.CancelDownload)
            else onAction(BuildDetailAction.DownloadOrInstallClicked)
        },
        enabled = enabled,
        contentDescription = label.replace("\n", " "),
        minHeight = 168.dp,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        if (downloading != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${(downloading.progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(52.dp)
                    .height(6.dp)
                    .background(AppTheme.colors.onActionTile.copy(alpha = .2f), RoundedCornerShape(3.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(downloading.progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(AppTheme.colors.onActionTile, RoundedCornerShape(3.dp)),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatMb(downloading.bytesLoaded)} / ${formatMb(build.fileSize)}",
                style = MonoMeta,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(6.dp))
        } else {
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text = label.uppercase(),
            style = MonoAction,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

private fun formatMb(bytes: Long): String = "${bytes / 1024 / 1024} МБ"

/** Версия разрезается по последней точке: голова в контенте, хвост в зоне. */
private fun String.versionHead(): String {
    val normalized = if (startsWith("v")) this else "v$this"
    val cut = normalized.lastIndexOf('.')
    return if (cut > 0) normalized.substring(0, cut) else normalized
}

private fun String.versionTail(): String {
    val cut = lastIndexOf('.')
    return if (cut > 0) substring(cut) else ""
}
