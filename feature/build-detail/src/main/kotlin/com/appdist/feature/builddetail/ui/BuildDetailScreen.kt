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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.AppError
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadState
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LedgerBadge
import com.appdist.core.ui.components.LedgerPrimaryButton
import com.appdist.core.ui.components.LedgerScreen
import com.appdist.core.ui.components.LedgerTopBar
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.core.ui.theme.AppTheme
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
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                is BuildDetailEffect.CopyToClipboard -> {
                    clipboard.setText(AnnotatedString(effect.text))
                    snackbarHostState.showSnackbar("Ссылка скопирована")
                }
                is BuildDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is BuildDetailEffect.ShowSignatureMismatchDialog -> signatureMismatch = effect.installedFp
                is BuildDetailEffect.ShowDowngradeWarning -> {
                    downgradeWarning = effect.installedCode to effect.newCode
                }
            }
        }
    }

    LedgerScreen {
        val build = state.build
        when {
            state.isLoading -> LoadingScreen(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
            state.error != null -> ErrorScreen(
                error = state.error!!,
                title = "Не удалось загрузить сборку",
                description = if (state.error is AppError.Network) {
                    "Сервер не вернул данные этой сборки. Попробуйте ещё раз."
                } else null,
                onRetry = { viewModel.onAction(BuildDetailAction.RetryClicked) },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
            build != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 126.dp),
                ) {
                    LedgerTopBar(
                        title = build.projectName,
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        leadingDescription = "Назад",
                        onLeadingClick = onBack,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            build.versionName.withVersionPrefix(),
                            style = MaterialTheme.typography.displayMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                "BUILD " + build.versionCode,
                                build.branch?.takeIf { it.isNotBlank() }?.let { "BR:" + it },
                            ).joinToString("  ·  "),
                            style = MonoMeta,
                            color = AppTheme.colors.muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (build.isLatestInChannel) LedgerBadge("latest", selected = true)
                            LedgerBadge(build.channel.ifBlank { "QA" })
                            LedgerBadge(build.environment.ifBlank { "internal" })
                        }
                        if (!build.changelog.isNullOrBlank()) {
                            ChangelogSection(
                                changelog = build.changelog.orEmpty(),
                                uploadDate = build.uploadDate,
                                fileSize = build.fileSize,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                        TechDetailsSection(build = build, modifier = Modifier.padding(top = 18.dp))
                        Spacer(Modifier.height(24.dp))
                    }
                }

                InstallAction(
                    build = build,
                    downloadState = state.downloadState,
                    onAction = viewModel::onAction,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
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
                state.build?.packageName?.let { context.openAppSettings(it) }
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
                state.build?.packageName?.let { context.openAppSettings(it) }
            },
        )
    }
}

@Composable
private fun InstallAction(
    build: BuildUi,
    downloadState: DownloadState,
    onAction: (BuildDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloading = downloadState as? DownloadState.Downloading
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (downloading != null) {
            val progress = downloading.progress.coerceIn(0f, 1f)
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(shape)
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.panelStrokeStrong, shape)
                    .clickable { onAction(BuildDetailAction.CancelDownload) },
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
                    "Загрузка · " + (progress * 100).toInt() + "%",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTheme.colors.ink,
                )
            }
            Text(
                formatMb(downloading.bytesLoaded) + " / " + formatMb(build.fileSize) +
                    "  ·  нажмите, чтобы отменить",
                style = MonoMeta,
                color = AppTheme.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
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
                build.installStatus is InstallStatus.UpdateAvailable -> "Обновить"
                build.installStatus is InstallStatus.Installed -> "Готово"
                build.installStatus is InstallStatus.InstalledNewer -> "Откатить"
                build.installStatus is InstallStatus.Incompatible -> "Недоступно"
                else -> "Скачать и установить"
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
                height = 64.dp,
                onClick = { onAction(BuildDetailAction.DownloadOrInstallClicked) },
            )
            Text(
                "APK · " + formatMb(build.fileSize),
                style = MonoMeta,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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
            data = Uri.parse("package:" + packageName)
        }
    )
}

private fun formatMb(bytes: Long): String = (bytes / 1024 / 1024).toString() + " МБ"

private fun String.withVersionPrefix(): String = if (startsWith("v")) this else "v$this"
