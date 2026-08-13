package com.appdist.feature.builddetail.domain

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.appdist.core.common.model.BuildUi
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.feature.builddetail.data.DownloadWorker
import javax.inject.Inject

/**
 * Ставит загрузку в очередь и ничего не возвращает.
 *
 * Наблюдать за результатом по UUID этого запроса нельзя: при
 * ExistingWorkPolicy.KEEP уже идущая работа сохраняется, а новый запрос
 * отбрасывается и в базе WorkManager не появляется. Состояние читается
 * через DownloadRepository.observeDownload по имени уникальной работы.
 */
class StartDownloadUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    operator fun invoke(build: BuildUi, downloadUrl: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorkKeys.KEY_BUILD_ID to build.id,
                    DownloadWorkKeys.KEY_DOWNLOAD_URL to downloadUrl,
                    DownloadWorkKeys.KEY_EXPECTED_CHECKSUM to build.checksumSha256,
                    DownloadWorkKeys.KEY_FILE_SIZE to build.fileSize,
                    DownloadWorkKeys.KEY_VERSION_NAME to build.versionName,
                    DownloadWorkKeys.KEY_VERSION_CODE to build.versionCode,
                    DownloadWorkKeys.KEY_CHANNEL to build.channel
                )
            )
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork(
            "download_${build.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
