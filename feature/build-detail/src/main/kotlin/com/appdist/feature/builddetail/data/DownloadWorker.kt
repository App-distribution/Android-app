package com.appdist.feature.builddetail.data

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.downloads.DownloadPaths
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.core.network.ApiService
import com.appdist.feature.builddetail.domain.VerifyChecksumUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Загрузка APK, которая не сдаётся: обрыв сети, истёкшая ссылка, остановка
 * системой — всё это повод продолжить с того же байта, а не начать заново.
 * Окончательно загрузка падает, только если сборки нет на сервере или не
 * совпала контрольная сумма.
 *
 * ВНИМАНИЕ: полное имя класса хранится в базе WorkManager. Перенос в другой
 * пакет или модуль сделает невозможным исполнение работ, поставленных в очередь
 * предыдущими версиями приложения.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val downloader: ResumableDownloader,
    private val api: ApiService,
    private val verifyChecksum: VerifyChecksumUseCase
) : CoroutineWorker(context, params) {

    companion object {
        /** Не чаще одной записи прогресса в БД и уведомление за этот интервал. */
        private const val PROGRESS_WRITE_INTERVAL_MS = 500L
        private const val CHANNEL_ID = "downloads"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val buildId = inputData.getString(DownloadWorkKeys.KEY_BUILD_ID)
            ?: return@withContext Result.failure()
        val initialUrl = inputData.getString(DownloadWorkKeys.KEY_DOWNLOAD_URL)
        val totalBytes = inputData.getLong(DownloadWorkKeys.KEY_FILE_SIZE, 0L)
        val expectedChecksum = inputData.getString(DownloadWorkKeys.KEY_EXPECTED_CHECKSUM)

        // Значения по умолчанию — для работ, поставленных в очередь до обновления:
        // у них этих ключей в inputData нет.
        val versionName = inputData.getString(DownloadWorkKeys.KEY_VERSION_NAME) ?: ""
        val versionCode = inputData.getLong(DownloadWorkKeys.KEY_VERSION_CODE, 0L)
        val channel = inputData.getString(DownloadWorkKeys.KEY_CHANNEL) ?: ""

        val partFile = DownloadPaths.partFile(applicationContext, buildId)
        val apkFile = DownloadPaths.apkFile(applicationContext, buildId)
        val startedAtMs = downloadDao.getByBuildId(buildId)?.startedAt ?: System.currentTimeMillis()

        fun row(state: String, loaded: Long, path: String? = null, completedAt: Long? = null) =
            DownloadEntity(
                buildId = buildId,
                localPath = path,
                state = state,
                progress = fraction(loaded, totalBytes),
                bytesLoaded = loaded,
                totalBytes = totalBytes,
                startedAt = startedAtMs,
                versionName = versionName,
                versionCode = versionCode,
                channel = channel,
                completedAt = completedAt
            )

        suspend fun fail(reason: String): Result {
            partFile.delete()
            apkFile.delete()
            downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0L))
            return Result.failure(workDataOf(DownloadWorkKeys.KEY_ERROR to reason))
        }

        val title = "Скачивание ${versionName.ifBlank { "сборки" }}"
        val alreadyOnDisk = if (partFile.exists()) partFile.length() else 0L
        promoteToForeground(title, alreadyOnDisk, totalBytes)

        try {
            downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, alreadyOnDisk))

            val url = when (val fresh = freshDownloadUrl(buildId)) {
                is UrlLookup.Found -> fresh.url
                UrlLookup.BuildGone -> return@withContext fail("Сборка удалена с сервера")
                // Нет связи с API — пробуем прежнюю ссылку, вдруг ещё жива.
                UrlLookup.Unavailable -> initialUrl ?: return@withContext Result.retry()
            }

            var lastWriteAt = 0L
            val transfer = downloader.download(url, partFile) { onDisk ->
                val now = System.currentTimeMillis()
                if (now - lastWriteAt >= PROGRESS_WRITE_INTERVAL_MS) {
                    lastWriteAt = now
                    publishProgress(onDisk, totalBytes)
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, onDisk))
                    promoteToForeground(title, onDisk, totalBytes)
                }
            }

            when (transfer) {
                is TransferResult.RetryLater -> {
                    Timber.w("Download of %s paused: %s", buildId, transfer.reason)
                    val onDisk = if (partFile.exists()) partFile.length() else 0L
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, onDisk))
                    return@withContext Result.retry()
                }
                is TransferResult.Fatal -> return@withContext fail(transfer.reason)
                is TransferResult.Completed -> Unit
            }

            // Финальный прогресс не троттлится, иначе 100% может не долететь до UI.
            publishProgress(partFile.length(), totalBytes)

            if (!expectedChecksum.isNullOrBlank()) {
                downloadDao.upsert(row(DownloadWorkKeys.STATE_VERIFYING, partFile.length()))
                if (verifyChecksum(partFile, expectedChecksum).isError) {
                    if (transfer.resumed) {
                        // Склейка из нескольких попыток могла разойтись — качаем начисто.
                        partFile.delete()
                        downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, 0L))
                        return@withContext Result.retry()
                    }
                    // Цельный файл не совпал с заявленной суммой — повтор не поможет.
                    return@withContext fail("Контрольная сумма не совпала")
                }
            }

            apkFile.delete()
            if (!partFile.renameTo(apkFile)) return@withContext Result.retry()

            downloadDao.upsert(
                row(DownloadWorkKeys.STATE_READY, apkFile.length(), apkFile.absolutePath, System.currentTimeMillis())
            )
            Result.success(workDataOf(DownloadWorkKeys.KEY_OUTPUT_PATH to apkFile.absolutePath))
        } catch (e: CancellationException) {
            // Отмена (пропала сеть, doze, cancelUniqueWork). partFile не трогаем:
            // при остановке системой докачка продолжится с этого места, а при
            // отмене пользователем файл удаляет DownloadRepository.cancel.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Download attempt failed for build %s", buildId)
            Result.retry()
        }
    }

    private sealed interface UrlLookup {
        data class Found(val url: String) : UrlLookup
        data object BuildGone : UrlLookup
        data object Unavailable : UrlLookup
    }

    /** Подписанная ссылка живёт 15 минут — на каждую попытку берём новую. */
    private suspend fun freshDownloadUrl(buildId: String): UrlLookup = try {
        val response = api.getDownloadUrl(buildId)
        when {
            response.isSuccessful -> response.body()?.url?.let(UrlLookup::Found) ?: UrlLookup.Unavailable
            response.code() == 404 -> UrlLookup.BuildGone
            else -> UrlLookup.Unavailable
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        UrlLookup.Unavailable
    }

    private suspend fun publishProgress(onDisk: Long, total: Long) = setProgress(
        workDataOf(
            DownloadWorkKeys.KEY_PROGRESS to fraction(onDisk, total),
            DownloadWorkKeys.KEY_BYTES_LOADED to onDisk
        )
    )

    /**
     * Foreground-сервис с уведомлением снимает 10-минутный лимит WorkManager
     * и не даёт системе остановить загрузку в фоне. Запуск foreground из фона
     * Android 12+ может запретить — тогда качаем как обычная работа.
     */
    private suspend fun promoteToForeground(title: String, onDisk: Long, total: Long) {
        try {
            setForeground(foregroundInfo(title, onDisk, total))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Foreground download not allowed, continuing in background")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo("Скачивание сборки", 0L, inputData.getLong(DownloadWorkKeys.KEY_FILE_SIZE, 0L))

    private fun foregroundInfo(title: String, onDisk: Long, total: Long): ForegroundInfo {
        val notificationId = id.hashCode()
        val notification = buildNotification(title, onDisk, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildNotification(title: String, onDisk: Long, total: Long): Notification {
        NotificationManagerCompat.from(applicationContext).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Загрузки сборок")
                .build()
        )
        val percent = (fraction(onDisk, total) * 100).toInt()
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (total > 0) "$percent%" else null)
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun fraction(loaded: Long, total: Long): Float =
        if (total > 0) (loaded.toFloat() / total).coerceIn(0f, 1f) else 0f
}
