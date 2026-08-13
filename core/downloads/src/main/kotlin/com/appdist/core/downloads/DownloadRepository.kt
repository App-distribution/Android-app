package com.appdist.core.downloads

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.appdist.core.common.AppError
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager
) {

    /**
     * ВНИМАНИЕ: формат имени не менять — по нему находятся работы,
     * поставленные в очередь предыдущими версиями приложения.
     */
    fun uniqueWorkName(buildId: String): String = "download_$buildId"

    /**
     * Состояние загрузки конкретной сборки.
     *
     * WorkManager отвечает за то, что происходит сейчас, Room — за то, что лежит
     * на диске. Наблюдение идёт по имени уникальной работы, а не по UUID запроса:
     * при ExistingWorkPolicy.KEEP новый запрос отбрасывается и его UUID в базе
     * WorkManager не появляется.
     */
    fun observeDownload(buildId: String): Flow<DownloadState> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(buildId)),
        downloadDao.observeByBuildId(buildId)
    ) { infos, entity ->
        toDownloadState(infos, entity)
    }.flowOn(Dispatchers.IO)

    /** Скачанные и лежащие на диске APK — для раздела «Скачанные». */
    fun observeDownloaded(): Flow<List<DownloadedApk>> = downloadDao.observeReadyDownloads()
        .map { rows -> rows.mapNotNull { it.toDownloadedApk() } }
        .flowOn(Dispatchers.IO)

    /** Отмена загрузки: снимаем работу, удаляем частично скачанный файл и строку. */
    suspend fun cancel(buildId: String) = withContext(Dispatchers.IO) {
        workManager.cancelUniqueWork(uniqueWorkName(buildId))
        deleteFileAndRow(buildId)
    }

    suspend fun delete(buildId: String) = withContext(Dispatchers.IO) {
        deleteFileAndRow(buildId)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        downloadDao.getReadyDownloads().forEach { deleteFileAndRow(it.buildId) }
    }

    /**
     * Строка может навсегда остаться в состоянии downloading, если процесс воркера
     * был убит. Такие строки переводим в failed, иначе они вечно висят в разделе
     * «Загрузки». Заодно чистим строки, чей файл система удалила из кэша.
     */
    suspend fun reconcileStaleDownloads() = withContext(Dispatchers.IO) {
        downloadDao.getStuckDownloads().forEach { row ->
            // Flow-вариант, а не getWorkInfosForUniqueWork(): тот возвращает
            // ListenableFuture и потребовал бы guava в зависимостях.
            val infos = workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(row.buildId)).first()
            val stillRunning = infos.any { !it.state.isFinished }
            if (!stillRunning) downloadDao.markFailed(row.buildId)
        }
        downloadDao.getReadyDownloads().forEach { row ->
            val path = row.localPath
            if (path == null || !File(path).exists()) {
                downloadDao.delete(row.buildId)
            }
        }
    }

    private suspend fun deleteFileAndRow(buildId: String) {
        val path = downloadDao.getByBuildId(buildId)?.localPath
        if (path != null) File(path).delete()
        DownloadPaths.apkFile(context, buildId).delete()
        downloadDao.delete(buildId)
    }

    private fun toDownloadState(infos: List<WorkInfo>, entity: DownloadEntity?): DownloadState {
        val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
        val readyFile = entity
            ?.takeIf { it.state == DownloadWorkKeys.STATE_READY || it.state == DownloadWorkKeys.STATE_LEGACY_COMPLETED }
            ?.localPath
            ?.let(::File)
            ?.takeIf { it.exists() }

        return when {
            info?.state == WorkInfo.State.RUNNING && entity?.state == DownloadWorkKeys.STATE_VERIFYING ->
                DownloadState.Verifying

            info?.state == WorkInfo.State.RUNNING -> DownloadState.Downloading(
                progress = info.progress.getFloat(DownloadWorkKeys.KEY_PROGRESS, entity?.progress ?: 0f),
                bytesLoaded = info.progress.getLong(DownloadWorkKeys.KEY_BYTES_LOADED, entity?.bytesLoaded ?: 0L)
            )

            info?.state == WorkInfo.State.ENQUEUED || info?.state == WorkInfo.State.BLOCKED ->
                DownloadState.Downloading(0f, 0L)

            readyFile != null -> DownloadState.ReadyToInstall(readyFile.absolutePath)

            info?.state == WorkInfo.State.FAILED -> DownloadState.Failed(
                AppError.Unknown(info.outputData.getString(DownloadWorkKeys.KEY_ERROR) ?: "Download failed")
            )

            else -> DownloadState.Idle
        }
    }

    private fun DownloadEntity.toDownloadedApk(): DownloadedApk? {
        val file = localPath?.let(::File) ?: return null
        if (!file.exists()) return null
        return DownloadedApk(
            buildId = buildId,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            versionName = versionName,
            versionCode = versionCode,
            channel = channel,
            completedAt = completedAt ?: startedAt
        )
    }
}
