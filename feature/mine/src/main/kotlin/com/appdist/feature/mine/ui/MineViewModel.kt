package com.appdist.feature.mine.ui

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadedApkUi(
    val buildId: String,
    val title: String,
    val channel: String,
    val sizeBytes: Long,
    val completedAt: Long
)

data class MineUiState(
    val installHistory: List<InstallHistoryEntity> = emptyList(),
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val downloaded: List<DownloadedApkUi> = emptyList()
) {
    val downloadedTotalBytes: Long get() = downloaded.sumOf { it.sizeBytes }
}

sealed interface MineAction {
    data class DeleteDownload(val buildId: String) : MineAction
    data object DeleteAllDownloads : MineAction
}

@HiltViewModel
class MineViewModel @Inject constructor(
    installHistoryDao: InstallHistoryDao,
    downloadDao: DownloadDao,
    private val buildDao: BuildDao,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val state: StateFlow<MineUiState> = combine(
        installHistoryDao.getAll(),
        downloadDao.getActiveDownloads(),
        downloadRepository.observeDownloaded()
    ) { history, downloads, downloaded ->
        MineUiState(
            installHistory = history,
            activeDownloads = downloads,
            downloaded = downloaded.map { it.toUi() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())

    fun onAction(action: MineAction) {
        when (action) {
            is MineAction.DeleteDownload -> viewModelScope.launch {
                downloadRepository.delete(action.buildId)
            }
            MineAction.DeleteAllDownloads -> viewModelScope.launch {
                downloadRepository.deleteAll()
            }
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
        val title = when {
            versionName.isNotBlank() -> "v$versionName ($versionCode)"
            cached != null -> "v${cached.versionName} (${cached.versionCode})"
            else -> "Сборка ${buildId.take(8)}"
        }
        val resolvedChannel = channel.ifBlank { cached?.channel.orEmpty() }
        return DownloadedApkUi(
            buildId = buildId,
            title = title,
            channel = resolvedChannel,
            sizeBytes = sizeBytes,
            completedAt = completedAt
        )
    }
}
