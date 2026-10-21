package com.appdist.core.downloads

import android.content.Context
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryTest {

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var dao: DownloadDao
    private lateinit var workManager: WorkManager
    private lateinit var repository: DownloadRepository

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.cacheDir } returns cacheDir
        dao = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        repository = DownloadRepository(context, dao, workManager)
    }

    @Test
    fun `running work maps to Downloading with progress`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.RUNNING, workDataOf(
                DownloadWorkKeys.KEY_PROGRESS to 0.42f,
                DownloadWorkKeys.KEY_BYTES_LOADED to 4200L
            ))))
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_DOWNLOADING, localPath = null)
        )

        repository.observeDownload("b1").test {
            val state = awaitItem()
            assertTrue(state is DownloadState.Downloading)
            assertEquals(0.42f, (state as DownloadState.Downloading).progress)
            assertEquals(4200L, state.bytesLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enqueued work maps to Downloading at zero`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED, Data.EMPTY)))
        every { dao.observeByBuildId("b1") } returns flowOf(null)

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Downloading(0f, 0L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Между попытками работа снова ENQUEUED. Раньше прогресс в этот момент
    // падал к нулю, и казалось, что загрузка началась заново.
    @Test
    fun `work waiting for retry keeps progress and is marked waiting`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED, Data.EMPTY)))
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_DOWNLOADING, localPath = null)
                .copy(progress = 0.4f, bytesLoaded = 40L)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Downloading(0.4f, 40L, waiting = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel removes the partially downloaded file`() = runTest {
        val part = File(cacheDir, "${DownloadPaths.DIR_NAME}/b1.apk.part").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(10))
        }

        repository.cancel("b1")

        assertTrue(!part.exists())
    }

    @Test
    fun `existing file with no work maps to ReadyToInstall`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_READY, localPath = apk.absolutePath)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.ReadyToInstall(apk.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy completed state is treated as ready`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_LEGACY_COMPLETED, localPath = apk.absolutePath)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.ReadyToInstall(apk.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ready row without file on disk maps to Idle`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_READY, localPath = "/nope/gone.apk")
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `running work with verifying row maps to Verifying`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.RUNNING, Data.EMPTY)))
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_VERIFYING, localPath = null)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Verifying, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed work with no ready file maps to Failed`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(
                workInfo(
                    WorkInfo.State.FAILED,
                    Data.EMPTY,
                    outputData = workDataOf(DownloadWorkKeys.KEY_ERROR to "HTTP 500")
                )
            ))
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_FAILED, localPath = null)
        )

        repository.observeDownload("b1").test {
            val state = awaitItem()
            assertTrue(state is DownloadState.Failed)
            assertEquals(
                AppError.Unknown("HTTP 500"),
                (state as DownloadState.Failed).reason
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unfinished work wins over finished one`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(
                workInfo(WorkInfo.State.FAILED, Data.EMPTY),
                workInfo(WorkInfo.State.RUNNING, workDataOf(
                    DownloadWorkKeys.KEY_PROGRESS to 0.1f,
                    DownloadWorkKeys.KEY_BYTES_LOADED to 10L
                ))
            ))
        every { dao.observeByBuildId("b1") } returns flowOf(null)

        repository.observeDownload("b1").test {
            assertTrue(awaitItem() is DownloadState.Downloading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes file and row`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        coEvery { dao.getByBuildId("b1") } returns entity(
            "b1", DownloadWorkKeys.STATE_READY, localPath = apk.absolutePath
        )

        repository.delete("b1")

        assertTrue(!apk.exists())
        coVerify { dao.delete("b1") }
    }

    @Test
    fun `observeDownloaded skips rows whose file is gone`() = runTest {
        val present = File(cacheDir, "apk_downloads/here.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(1024))
        }
        every { dao.observeReadyDownloads() } returns flowOf(
            listOf(
                entity("here", DownloadWorkKeys.STATE_READY, localPath = present.absolutePath),
                entity("gone", DownloadWorkKeys.STATE_READY, localPath = "/nope/gone.apk")
            )
        )

        repository.observeDownloaded().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("here", items[0].buildId)
            assertEquals(1024L, items[0].sizeBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reconcile marks stuck downloads as failed when no work is running`() = runTest {
        coEvery { dao.getStuckDownloads() } returns listOf(
            entity("stuck", DownloadWorkKeys.STATE_DOWNLOADING, localPath = null)
        )
        coEvery { dao.getReadyDownloads() } returns emptyList()
        every { workManager.getWorkInfosForUniqueWorkFlow("download_stuck") } returns flowOf(emptyList())

        repository.reconcileStaleDownloads()

        coVerify { dao.markFailed("stuck") }
    }

    private fun workInfo(state: WorkInfo.State, progress: Data, outputData: Data = Data.EMPTY) = WorkInfo(
        id = UUID.randomUUID(),
        state = state,
        tags = emptySet(),
        outputData = outputData,
        progress = progress
    )

    private fun entity(buildId: String, state: String, localPath: String?) = DownloadEntity(
        buildId = buildId,
        localPath = localPath,
        state = state,
        progress = 0f,
        bytesLoaded = 0L,
        totalBytes = 100L,
        startedAt = 1000L,
        versionName = "1.0",
        versionCode = 1L,
        channel = "alpha",
        completedAt = 2000L
    )
}
