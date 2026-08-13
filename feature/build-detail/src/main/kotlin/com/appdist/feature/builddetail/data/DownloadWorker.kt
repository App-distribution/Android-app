package com.appdist.feature.builddetail.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.downloads.DownloadPaths
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.feature.builddetail.domain.VerifyChecksumUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * ВНИМАНИЕ: полное имя класса хранится в базе WorkManager. Перенос в другой
 * пакет или модуль сделает невозможным исполнение работ, поставленных в очередь
 * предыдущими версиями приложения.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    private val verifyChecksum: VerifyChecksumUseCase
) : CoroutineWorker(context, params) {

    companion object {
        /** Не чаще одной записи прогресса в БД за этот интервал. */
        private const val PROGRESS_WRITE_INTERVAL_MS = 250L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val buildId = inputData.getString(DownloadWorkKeys.KEY_BUILD_ID)
            ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(DownloadWorkKeys.KEY_DOWNLOAD_URL)
            ?: return@withContext Result.failure()
        val totalBytes = inputData.getLong(DownloadWorkKeys.KEY_FILE_SIZE, 0L)
        val expectedChecksum = inputData.getString(DownloadWorkKeys.KEY_EXPECTED_CHECKSUM)

        // Значения по умолчанию — для работ, поставленных в очередь до обновления:
        // у них этих ключей в inputData нет.
        val versionName = inputData.getString(DownloadWorkKeys.KEY_VERSION_NAME) ?: ""
        val versionCode = inputData.getLong(DownloadWorkKeys.KEY_VERSION_CODE, 0L)
        val channel = inputData.getString(DownloadWorkKeys.KEY_CHANNEL) ?: ""

        val outputFile = DownloadPaths.apkFile(applicationContext, buildId)
            .also { it.parentFile?.mkdirs() }

        val startedAtMs = System.currentTimeMillis()

        fun row(state: String, progress: Float, loaded: Long, path: String?, completedAt: Long?) =
            DownloadEntity(
                buildId = buildId,
                localPath = path,
                state = state,
                progress = progress,
                bytesLoaded = loaded,
                totalBytes = totalBytes,
                startedAt = startedAtMs,
                versionName = versionName,
                versionCode = versionCode,
                channel = channel,
                completedAt = completedAt
            )

        try {
            downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, 0f, 0L, null, null))

            val request = Request.Builder().url(downloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    outputFile.delete()
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
                    return@withContext if (response.code in 400..499) {
                        Result.failure(workDataOf(DownloadWorkKeys.KEY_ERROR to "HTTP ${response.code}"))
                    } else {
                        Result.retry()
                    }
                }
                val body = response.body
                if (body == null) {
                    outputFile.delete()
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
                    return@withContext Result.retry()
                }

                outputFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastWriteAt = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                            // setProgress пишет в БД самого WorkManager (отдельная
                            // транзакция, инвалидирующая наблюдателей
                            // getWorkInfosForUniqueWorkFlow) — троттлим его в той же
                            // паре с записью в appdist.db, иначе на 14 МБ выходит
                            // ~1700 записей в обе базы и столько же эмиссий в UI.
                            val now = System.currentTimeMillis()
                            if (now - lastWriteAt >= PROGRESS_WRITE_INTERVAL_MS) {
                                lastWriteAt = now
                                setProgress(
                                    workDataOf(
                                        DownloadWorkKeys.KEY_PROGRESS to progress,
                                        DownloadWorkKeys.KEY_BYTES_LOADED to totalRead
                                    )
                                )
                                downloadDao.upsert(
                                    row(DownloadWorkKeys.STATE_DOWNLOADING, progress, totalRead, null, null)
                                )
                            }
                        }
                        // Финальный прогресс не троттлится, иначе 100% может не
                        // долететь до UI, если последний чанк попал внутрь окна.
                        val finalProgress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        setProgress(
                            workDataOf(
                                DownloadWorkKeys.KEY_PROGRESS to finalProgress,
                                DownloadWorkKeys.KEY_BYTES_LOADED to totalRead
                            )
                        )
                    }
                }
            }

            if (!expectedChecksum.isNullOrBlank()) {
                downloadDao.upsert(row(DownloadWorkKeys.STATE_VERIFYING, 1f, totalBytes, null, null))
                val verified = verifyChecksum(outputFile, expectedChecksum)
                if (verified.isError) {
                    outputFile.delete()
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
                    // Повтор не поможет: файл на сервере не совпадает с заявленной суммой.
                    return@withContext Result.failure(
                        workDataOf(DownloadWorkKeys.KEY_ERROR to "Контрольная сумма не совпала")
                    )
                }
            }

            downloadDao.upsert(
                row(
                    DownloadWorkKeys.STATE_READY,
                    1f,
                    totalBytes,
                    outputFile.absolutePath,
                    System.currentTimeMillis()
                )
            )
            Result.success(workDataOf(DownloadWorkKeys.KEY_OUTPUT_PATH to outputFile.absolutePath))
        } catch (e: CancellationException) {
            // Не наш сбой, а отмена (constraint lost, doze, лимит в 10 минут,
            // cancelUniqueWork). Проглатывание здесь запускало бы upsert в уже
            // отменённом scope — suspend-вызов упал бы с исключением вместо
            // записи failed, и строка вечно висела бы в состоянии downloading.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Download failed for build %s", buildId)
            outputFile.delete()
            downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
            Result.retry()
        }
    }
}
