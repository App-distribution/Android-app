package com.appdist.core.downloads

import com.appdist.core.common.AppError

sealed interface DownloadState {
    data object Idle : DownloadState
    /** [waiting] — попытка оборвалась, ждём сеть или паузу перед повтором; скачанное сохранено. */
    data class Downloading(
        val progress: Float,
        val bytesLoaded: Long,
        val waiting: Boolean = false,
    ) : DownloadState
    data object Verifying : DownloadState
    data class ReadyToInstall(val filePath: String) : DownloadState
    data class Failed(val reason: AppError) : DownloadState
}

/** Скачанный и проверенный APK, лежащий на диске. */
data class DownloadedApk(
    val buildId: String,
    val filePath: String,
    val sizeBytes: Long,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val completedAt: Long
)
