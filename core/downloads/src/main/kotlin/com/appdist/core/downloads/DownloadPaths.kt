package com.appdist.core.downloads

import android.content.Context
import java.io.File

/**
 * Единственное место, задающее расположение скачанных APK.
 *
 * ВНИМАНИЕ: путь совпадает с объявленным в app/src/main/res/xml/file_paths.xml
 * (<cache-path name="apk_downloads" path="apk_downloads/" />). Менять нельзя —
 * сломается выдача URI установщику и потеряются уже скачанные файлы.
 */
object DownloadPaths {
    const val DIR_NAME = "apk_downloads"

    fun downloadsDir(context: Context): File = File(context.cacheDir, DIR_NAME)

    fun apkFile(context: Context, buildId: String): File = File(downloadsDir(context), "$buildId.apk")
}
