package com.appdist.feature.mine.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadedApk
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class DownloadedApkUi(
    val buildId: String,
    val title: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val filePath: String,
    val sizeBytes: Long,
    val completedAt: Long
)

data class MineUiState(
    val installHistory: List<InstallHistoryEntity> = emptyList(),
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val downloaded: List<DownloadedApkUi> = emptyList(),
    val freeBytes: Long = 0L
) {
    val downloadedTotalBytes: Long get() = downloaded.sumOf { it.sizeBytes }

    /** Доля локальных APK в доступном месте — дорожка хранилища. */
    val storageFraction: Float
        get() {
            val total = downloadedTotalBytes + freeBytes
            return if (total <= 0L) 0f else (downloadedTotalBytes.toFloat() / total)
        }
}

sealed interface MineAction {
    data class DeleteDownload(val buildId: String) : MineAction
    data class InstallDownload(val buildId: String) : MineAction
    data object DeleteAllDownloads : MineAction
}

sealed interface MineEffect {
    data class LaunchInstaller(val apkUri: Uri) : MineEffect
    data class ShowMessage(val text: String) : MineEffect
}

@HiltViewModel
class MineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    installHistoryDao: InstallHistoryDao,
    downloadDao: DownloadDao,
    private val buildDao: BuildDao,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _effects = Channel<MineEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<MineUiState> = combine(
        installHistoryDao.getAll(),
        downloadDao.getActiveDownloads(),
        downloadRepository.observeDownloaded()
    ) { history, downloads, downloaded ->
        MineUiState(
            installHistory = history,
            activeDownloads = downloads,
            downloaded = downloaded.map { it.toUi() },
            freeBytes = runCatching { context.cacheDir.usableSpace }.getOrDefault(0L)
        )
    }
        .catch { e ->
            // Трансформация теперь делает suspend-вызов в БД (getBuildById);
            // сбой БД здесь не должен уходить необработанным из stateIn и
            // ронять процесс — экран просто перестанет обновляться.
            Timber.e(e, "Failed to combine Mine screen state")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())

    fun onAction(action: MineAction) {
        when (action) {
            is MineAction.DeleteDownload -> viewModelScope.launch {
                try {
                    downloadRepository.delete(action.buildId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete download %s", action.buildId)
                }
            }
            is MineAction.InstallDownload -> viewModelScope.launch { install(action.buildId) }
            MineAction.DeleteAllDownloads -> viewModelScope.launch {
                try {
                    downloadRepository.deleteAll()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete all downloads")
                }
            }
        }
    }

    /**
     * Установка прямо из «Скачанных»: файл уже проверен воркером, поэтому
     * здесь достаточно отдать его установщику через FileProvider.
     */
    private suspend fun install(buildId: String) {
        val apk = state.value.downloaded.firstOrNull { it.buildId == buildId }
        val file = apk?.filePath?.let(::File)
        if (file == null || !file.exists()) {
            _effects.send(MineEffect.ShowMessage("Файл сборки не найден — скачайте её заново"))
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            _effects.send(MineEffect.LaunchInstaller(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to build installer URI for %s", buildId)
            _effects.send(MineEffect.ShowMessage("Не удалось открыть установщик"))
        }
    }

    /**
     * У строк, созданных версиями до миграции 1 → 2, метаданных сборки нет:
     * versionName, channel пустые, versionCode нулевой. Подставляем по цепочке
     * строка → кэш builds → короткий идентификатор (для title; channel просто
     * остаётся пустым, если и в кэше ничего нет).
     */
    private suspend fun DownloadedApk.toUi(): DownloadedApkUi {
        val cached = if (versionName.isBlank() || channel.isBlank()) buildDao.getBuildById(buildId) else null
        val resolvedVersion = versionName.ifBlank { cached?.versionName.orEmpty() }
        val resolvedCode = if (versionCode != 0L) versionCode else cached?.versionCode ?: 0L
        val title = when {
            resolvedVersion.isNotBlank() -> "v$resolvedVersion ($resolvedCode)"
            else -> "Сборка ${buildId.take(8)}"
        }
        return DownloadedApkUi(
            buildId = buildId,
            title = title,
            versionName = resolvedVersion,
            versionCode = resolvedCode,
            channel = channel.ifBlank { cached?.channel.orEmpty() },
            filePath = filePath,
            sizeBytes = sizeBytes,
            completedAt = completedAt
        )
    }
}
