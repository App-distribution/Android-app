package com.appdist.feature.mine

import app.cash.turbine.test
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadedApk
import com.appdist.feature.mine.ui.MineAction
import com.appdist.feature.mine.ui.MineViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var installHistoryDao: InstallHistoryDao
    private lateinit var downloadDao: DownloadDao
    private lateinit var buildDao: BuildDao
    private lateinit var downloadRepository: DownloadRepository

    private val fakeHistory = listOf(
        InstallHistoryEntity("id1", "b1", "com.test", "1.0", 1L, System.currentTimeMillis())
    )
    private val fakeDownloads = listOf(
        DownloadEntity("b2", null, "downloading", 0.5f, 512L, 1024L, System.currentTimeMillis())
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        installHistoryDao = mockk()
        downloadDao = mockk()
        buildDao = mockk()
        downloadRepository = mockk(relaxed = true)
        every { installHistoryDao.getAll() } returns flowOf(fakeHistory)
        every { downloadDao.getActiveDownloads() } returns flowOf(fakeDownloads)
        every { downloadRepository.observeDownloaded() } returns flowOf(emptyList())
        coEvery { buildDao.getBuildById(any()) } returns null
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel() =
        MineViewModel(installHistoryDao, downloadDao, buildDao, downloadRepository)

    @Test
    fun `combines install history and active downloads`() = runTest {
        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(1, state.installHistory.size)
            assertEquals("com.test", state.installHistory[0].packageName)
            assertEquals(1, state.activeDownloads.size)
            assertEquals("b2", state.activeDownloads[0].buildId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `downloaded section shows version from row and sums total size`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(
                downloadedApk("d1", versionName = "2.1", versionCode = 21L, size = 1024L),
                downloadedApk("d2", versionName = "2.2", versionCode = 22L, size = 2048L)
            )
        )

        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(2, state.downloaded.size)
            assertEquals("v2.1 (21)", state.downloaded[0].title)
            assertEquals(3072L, state.downloadedTotalBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row without version falls back to builds cache`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("legacy", versionName = "", versionCode = 0L, size = 10L))
        )
        coEvery { buildDao.getBuildById("legacy") } returns buildEntity("legacy", "3.0", 30L)

        createViewModel().state.test {
            assertEquals("v3.0 (30)", awaitItem().downloaded[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row with nothing known falls back to short build id`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("0123456789abcdef", versionName = "", versionCode = 0L, size = 10L))
        )
        coEvery { buildDao.getBuildById("0123456789abcdef") } returns null

        createViewModel().state.test {
            assertEquals("Сборка 01234567", awaitItem().downloaded[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row without channel falls back to builds cache`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("legacy-channel", versionName = "2.1", versionCode = 21L, size = 10L, channel = ""))
        )
        coEvery { buildDao.getBuildById("legacy-channel") } returns
            buildEntity("legacy-channel", "2.1", 21L, channel = "beta")

        createViewModel().state.test {
            assertEquals("beta", awaitItem().downloaded[0].channel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row without channel and no cache hit leaves channel blank, not a dangling separator`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("no-cache", versionName = "2.1", versionCode = 21L, size = 10L, channel = ""))
        )
        coEvery { buildDao.getBuildById("no-cache") } returns null

        createViewModel().state.test {
            assertEquals("", awaitItem().downloaded[0].channel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete download delegates to repository and list shrinks`() = runTest {
        val downloaded = MutableStateFlow(
            listOf(
                downloadedApk("d1", versionName = "2.1", versionCode = 21L, size = 1024L),
                downloadedApk("d2", versionName = "2.2", versionCode = 22L, size = 2048L)
            )
        )
        every { downloadRepository.observeDownloaded() } returns downloaded

        val vm = createViewModel()
        vm.state.test {
            assertEquals(2, awaitItem().downloaded.size)

            vm.onAction(MineAction.DeleteDownload("d1"))
            coVerify { downloadRepository.delete("d1") }

            downloaded.value = downloaded.value.filterNot { it.buildId == "d1" }
            val after = awaitItem()
            assertEquals(1, after.downloaded.size)
            assertEquals(2048L, after.downloadedTotalBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete all downloads delegates to repository`() = runTest {
        val vm = createViewModel()
        vm.onAction(MineAction.DeleteAllDownloads)
        coVerify { downloadRepository.deleteAll() }
    }

    private fun downloadedApk(
        buildId: String,
        versionName: String,
        versionCode: Long,
        size: Long,
        channel: String = "alpha"
    ) = DownloadedApk(
        buildId = buildId,
        filePath = "/data/$buildId.apk",
        sizeBytes = size,
        versionName = versionName,
        versionCode = versionCode,
        channel = channel,
        completedAt = 1000L
    )

    private fun buildEntity(
        id: String,
        versionName: String,
        versionCode: Long,
        channel: String = "alpha"
    ) = BuildEntity(
        id = id, projectId = "p1", versionName = versionName, versionCode = versionCode,
        channel = channel, environment = "qa", changelog = null, fileSize = 10L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", cachedAt = 0L
    )
}
