package com.appdist.feature.builddetail

import com.appdist.feature.builddetail.data.ResumableDownloader
import com.appdist.feature.builddetail.data.TransferResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

class ResumableDownloaderTest {

    @TempDir
    lateinit var dir: File

    private val server = MockWebServer()
    private val downloader = ResumableDownloader(
        OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
    )
    private val apk = ByteArray(64 * 1024) { (it % 251).toByte() }
    private lateinit var part: File

    @BeforeEach
    fun setUp() {
        server.start()
        part = File(dir, "b1.apk.part")
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun url() = server.url("/b1.apk").toString()
    private fun body(from: Int = 0) = Buffer().write(apk, from, apk.size - from)

    @Test
    fun `fresh download writes the whole file`() = runTest {
        server.enqueue(MockResponse().setBody(body()))

        val result = downloader.download(url(), part) {}

        assertEquals(TransferResult.Completed(resumed = false), result)
        assertArrayEquals(apk, part.readBytes())
        assertNull(server.takeRequest().getHeader("Range"))
    }

    @Test
    fun `existing part is continued with a Range request and appended`() = runTest {
        part.writeBytes(apk.copyOfRange(0, 10_000))
        server.enqueue(
            MockResponse().setResponseCode(206)
                .setHeader("Content-Range", "bytes 10000-${apk.size - 1}/${apk.size}")
                .setBody(body(10_000))
        )

        val result = downloader.download(url(), part) {}

        assertEquals(TransferResult.Completed(resumed = true), result)
        assertEquals("bytes=10000-", server.takeRequest().getHeader("Range"))
        assertArrayEquals(apk, part.readBytes())
    }

    @Test
    fun `server ignoring Range starts the file over instead of appending`() = runTest {
        part.writeBytes(apk.copyOfRange(0, 10_000))
        server.enqueue(MockResponse().setResponseCode(200).setBody(body()))

        val result = downloader.download(url(), part) {}

        assertEquals(TransferResult.Completed(resumed = false), result)
        assertArrayEquals(apk, part.readBytes())
    }

    // Главный сценарий: сеть пропала посреди файла — скачанное не теряется.
    @Test
    fun `connection cut mid-file keeps what was received and asks to retry`() = runTest {
        server.enqueue(
            MockResponse().setBody(body())
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )

        val result = downloader.download(url(), part) {}

        assertInstanceOf(TransferResult.RetryLater::class.java, result)
        assertTrue(part.exists())
        assertTrue(part.length() in 1 until apk.size, "part=${part.length()}")
        val received = part.readBytes()
        assertArrayEquals(apk.copyOfRange(0, received.size), received)
    }

    // Подписанная ссылка MinIO живёт 15 минут: после этого 403, и это
    // повод взять новую ссылку, а не бросать загрузку.
    @Test
    fun `expired link is retried, not treated as final`() = runTest {
        part.writeBytes(apk.copyOfRange(0, 1_000))
        server.enqueue(MockResponse().setResponseCode(403))

        val result = downloader.download(url(), part) {}

        assertInstanceOf(TransferResult.RetryLater::class.java, result)
        assertEquals(1_000, part.length(), "скачанное сохраняется до следующей попытки")
    }

    @Test
    fun `missing file on the server is final`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        assertInstanceOf(TransferResult.Fatal::class.java, downloader.download(url(), part) {})
    }

    @Test
    fun `range past the end discards the part and retries from scratch`() = runTest {
        part.writeBytes(ByteArray(apk.size + 10))
        server.enqueue(MockResponse().setResponseCode(416))

        val result = downloader.download(url(), part) {}

        assertInstanceOf(TransferResult.RetryLater::class.java, result)
        assertFalse(part.exists())
    }

    @Test
    fun `mismatched Content-Range does not corrupt the file`() = runTest {
        part.writeBytes(apk.copyOfRange(0, 10_000))
        server.enqueue(
            MockResponse().setResponseCode(206)
                .setHeader("Content-Range", "bytes 5000-${apk.size - 1}/${apk.size}")
                .setBody(body(5_000))
        )

        val result = downloader.download(url(), part) {}

        assertInstanceOf(TransferResult.RetryLater::class.java, result)
        assertFalse(part.exists())
    }

    @Test
    fun `progress reports total bytes on disk including the resumed part`() = runTest {
        part.writeBytes(apk.copyOfRange(0, 10_000))
        server.enqueue(
            MockResponse().setResponseCode(206)
                .setHeader("Content-Range", "bytes 10000-${apk.size - 1}/${apk.size}")
                .setBody(body(10_000))
        )
        val seen = mutableListOf<Long>()

        downloader.download(url(), part) { seen += it }

        assertEquals(apk.size.toLong(), seen.last())
        assertTrue(seen.first() > 10_000)
    }
}
