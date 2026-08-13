package com.appdist.core.downloads

/**
 * Ключи inputData/progress воркера и значения состояний в таблице downloads.
 *
 * ВНИМАНИЕ: строковые значения существующих ключей менять нельзя — их читают
 * работы, поставленные в очередь предыдущими версиями приложения.
 */
object DownloadWorkKeys {
    const val KEY_BUILD_ID = "build_id"
    const val KEY_DOWNLOAD_URL = "download_url"
    const val KEY_EXPECTED_CHECKSUM = "expected_checksum"
    const val KEY_FILE_SIZE = "file_size"
    const val KEY_OUTPUT_PATH = "output_path"
    const val KEY_PROGRESS = "progress"
    const val KEY_BYTES_LOADED = "bytes_loaded"

    // Добавлены в версии 2 — у старых работ отсутствуют, читаются со значениями по умолчанию.
    const val KEY_VERSION_NAME = "version_name"
    const val KEY_VERSION_CODE = "version_code"
    const val KEY_CHANNEL = "channel"
    const val KEY_ERROR = "error"

    const val STATE_DOWNLOADING = "downloading"
    const val STATE_VERIFYING = "verifying"
    const val STATE_READY = "ready"
    const val STATE_FAILED = "failed"

    /** Записывалось воркером до миграции 1 → 2. Читается как «готово». */
    const val STATE_LEGACY_COMPLETED = "completed"
}
