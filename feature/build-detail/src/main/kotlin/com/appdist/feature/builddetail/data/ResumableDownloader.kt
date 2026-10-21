package com.appdist.feature.builddetail.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

sealed interface TransferResult {
    /** [resumed] — файл собран из нескольких попыток. */
    data class Completed(val resumed: Boolean) : TransferResult

    /** Сбой, который пройдёт сам: сеть, истёкшая ссылка, ошибка сервера. */
    data class RetryLater(val reason: String) : TransferResult

    /** Повтор не поможет: файла на сервере нет или запрос отвергнут. */
    data class Fatal(val reason: String) : TransferResult
}

/**
 * Качает файл в [partFile], продолжая с того места, где оборвалась прошлая
 * попытка (HTTP Range). Скачанное удаляется, только когда ему нельзя доверять:
 * сервер вернул не тот кусок или Range вышел за конец файла.
 */
class ResumableDownloader @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun download(
        url: String,
        partFile: File,
        onProgress: suspend (bytesOnDisk: Long) -> Unit
    ): TransferResult = withContext(Dispatchers.IO) {
        partFile.parentFile?.mkdirs()
        val existing = if (partFile.exists()) partFile.length() else 0L

        val request = Request.Builder().url(url).apply {
            if (existing > 0) header("Range", "bytes=$existing-")
        }.build()

        try {
            client.newCall(request).execute().use { response ->
                val append = when (response.code) {
                    206 -> {
                        if (rangeStart(response.header("Content-Range")) != existing) {
                            partFile.delete()
                            return@withContext TransferResult.RetryLater("unexpected Content-Range")
                        }
                        true
                    }
                    // Сервер проигнорировал Range — отдаёт файл целиком.
                    200 -> false
                    416 -> {
                        partFile.delete()
                        return@withContext TransferResult.RetryLater("range not satisfiable")
                    }
                    // Подписанная ссылка истекла: воркер возьмёт новую.
                    401, 403, 410 -> return@withContext TransferResult.RetryLater("HTTP ${response.code}")
                    in 400..499 -> return@withContext TransferResult.Fatal("HTTP ${response.code}")
                    else -> return@withContext TransferResult.RetryLater("HTTP ${response.code}")
                }

                val body = response.body ?: return@withContext TransferResult.RetryLater("empty body")
                var onDisk = if (append) existing else 0L
                FileOutputStream(partFile, append).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            onDisk += read
                            onProgress(onDisk)
                        }
                    }
                }
                TransferResult.Completed(resumed = append)
            }
        } catch (e: IOException) {
            // Всё, что успело записаться, остаётся в partFile для следующей попытки.
            TransferResult.RetryLater(e.message ?: e.javaClass.simpleName)
        }
    }

    /** "bytes 10000-65535/65536" → 10000. */
    private fun rangeStart(contentRange: String?): Long? =
        contentRange?.removePrefix("bytes ")?.substringBefore('-')?.trim()?.toLongOrNull()

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
