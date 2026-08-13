package com.appdist.feature.builddetail

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.BackendException
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadState
import com.appdist.feature.builddetail.domain.*
import com.appdist.feature.builddetail.ui.BuildDetailAction
import com.appdist.feature.builddetail.ui.BuildDetailEffect
import com.appdist.feature.builddetail.ui.BuildDetailViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getBuildDetail: GetBuildDetailUseCase
    private lateinit var getInstallStatus: GetInstallStatusUseCase
    private lateinit var startDownload: StartDownloadUseCase
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var reportInstall: ReportInstallUseCase
    private lateinit var context: android.content.Context

    private val fakeBuild = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0", versionCode = 1L, channel = "alpha", environment = "dev",
        buildType = "debug", changelog = null, fileSize = 1024L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", minSdk = 24, targetSdk = 35,
        certFingerprint = null, abis = emptyList(), expiryDate = null,
        branch = null, commitHash = null
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getBuildDetail = mockk()
        getInstallStatus = mockk()
        startDownload = mockk(relaxed = true)
        downloadRepository = mockk(relaxed = true)
        reportInstall = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.packageName } returns "com.appdist.app"

        every { getBuildDetail(any<String>()) } returns flowOf(fakeBuild)
        every { getInstallStatus(any(), any(), any(), any()) } returns InstallStatus.NotInstalled
        every { downloadRepository.observeDownload("b1") } returns flowOf(DownloadState.Idle)

        // FileProvider.getUriForFile parses AndroidX/AndroidManifest metadata through the real
        // PackageManager. Without Robolectric a relaxed Context mock has no manifest to read,
        // and the resulting fake XmlResourceParser never reports end-of-document, spinning the
        // real implementation forever. Stub the static call so tests exercise our own logic only.
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockk<Uri>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(FileProvider::class)
    }

    private fun createViewModel() = BuildDetailViewModel(
        SavedStateHandle(mapOf("buildId" to "b1")),
        context, getBuildDetail, getInstallStatus, startDownload, downloadRepository, reportInstall
    )

    @Test
    fun `loads build on init`() = runTest {
        createViewModel().state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("b1", state.build?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `copy link emits CopyToClipboard effect`() = runTest {
        val vm = createViewModel()
        vm.effects.test {
            vm.onAction(BuildDetailAction.CopyLinkClicked)
            val effect = awaitItem()
            assertTrue(effect is BuildDetailEffect.CopyToClipboard)
            assertEquals("appdist://builds/b1", (effect as BuildDetailEffect.CopyToClipboard).text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state set when build load fails`() = runTest {
        every { getBuildDetail(any<String>()) } returns flow { throw BackendException(503, "Unavailable") }
        createViewModel().state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(503, (state.error as AppError.Network).code)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Регрессия на баг: возврат на экран во время активной загрузки ---

    @Test
    fun `restores Downloading state on fresh view model when work is running`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.Downloading(0.35f, 350L))

        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(DownloadState.Downloading(0.35f, 350L), state.downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restores ReadyToInstall state without launching installer automatically`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.ReadyToInstall("/data/b1.apk"))

        val vm = createViewModel()
        vm.state.test {
            assertEquals(DownloadState.ReadyToInstall("/data/b1.apk"), awaitItem().downloadState)
            cancelAndIgnoreRemainingEvents()
        }
        // Восстановленное состояние не должно само открывать системный установщик
        coVerify(exactly = 0) { reportInstall(any()) }
    }

    @Test
    fun `does not enqueue a second download when file is already downloaded`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.ReadyToInstall("/data/b1.apk"))

        val vm = createViewModel()
        vm.onAction(BuildDetailAction.DownloadOrInstallClicked)

        verify(exactly = 0) { startDownload(any(), any()) }
    }

    @Test
    fun `button click enqueues download and state follows repository`() = runTest {
        val downloadFlow = MutableStateFlow<DownloadState>(DownloadState.Idle)
        every { downloadRepository.observeDownload("b1") } returns downloadFlow
        coEvery { getBuildDetail.getDownloadUrl("b1") } returns Result.Success("https://cdn/b1.apk")

        val vm = createViewModel()
        vm.state.test {
            assertEquals(DownloadState.Idle, awaitItem().downloadState)

            vm.onAction(BuildDetailAction.DownloadOrInstallClicked)
            verify { startDownload(fakeBuild, "https://cdn/b1.apk") }

            // Прогресс приходит из подписки, а не из результата постановки в очередь
            downloadFlow.value = DownloadState.Downloading(0.5f, 500L)
            assertEquals(DownloadState.Downloading(0.5f, 500L), awaitItem().downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel delegates to repository`() = runTest {
        val vm = createViewModel()
        vm.onAction(BuildDetailAction.CancelDownload)
        coVerify { downloadRepository.cancel("b1") }
    }
}
