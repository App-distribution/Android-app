package com.appdist.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadState
import com.appdist.feature.browse.domain.BuildFilterOptions
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.domain.GetBuildsUseCase
import com.appdist.feature.browse.ui.builds.BuildFilters
import com.appdist.feature.browse.ui.builds.BuildsViewModel
import com.appdist.feature.builddetail.domain.GetInstallStatusUseCase
import com.appdist.feature.builddetail.domain.ReportInstallUseCase
import com.appdist.feature.builddetail.domain.StartDownloadUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: BuildRepository
    private lateinit var getInstallStatus: GetInstallStatusUseCase
    private lateinit var startDownload: StartDownloadUseCase
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var viewModel: BuildsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.getBuilds(any(), any()) } returns flowOf(PagingData.empty())
        coEvery { repository.getFilterOptions(any()) } returns
            Result.Success(BuildFilterOptions(flavors = listOf("stage", "prod"), stands = listOf("l10")))
        getInstallStatus = mockk()
        startDownload = mockk(relaxed = true)
        downloadRepository = mockk(relaxed = true)
        val useCase = GetBuildsUseCase(repository)
        val savedState = SavedStateHandle(mapOf("projectId" to "proj-1"))
        viewModel = BuildsViewModel(
            getBuilds = useCase,
            buildRepository = repository,
            context = mockk(relaxed = true),
            getInstallStatus = getInstallStatus,
            startDownload = startDownload,
            downloadRepository = downloadRepository,
            reportInstall = mockk<ReportInstallUseCase>(relaxed = true),
            savedStateHandle = savedState,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial filters are empty`() = runTest {
        viewModel.filters.test {
            val filters = awaitItem()
            assertNull(filters.flavor)
            assertNull(filters.stand)
            assertEquals("", filters.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flavor and stand combine`() = runTest {
        viewModel.filters.test {
            awaitItem() // initial
            viewModel.setFlavor("stage")
            assertEquals("stage", awaitItem().flavor)
            viewModel.setStand("l10")
            val filters = awaitItem()
            assertEquals("stage" to "l10", filters.flavor to filters.stand)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter chips come from the project's real values`() = runTest {
        viewModel.filterOptions.test {
            assertEquals(
                BuildFilterOptions(flavors = listOf("stage", "prod"), stands = listOf("l10")),
                expectMostRecentItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearFilters resets to defaults`() = runTest {
        viewModel.setFlavor("stage")
        viewModel.setStand("l10")
        viewModel.clearFilters()
        viewModel.filters.test {
            val filters = awaitItem()
            assertTrue(filters.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `latest install click starts download without opening detail`() = runTest {
        val build = build()
        every {
            getInstallStatus(
                packageName = "ru.example.app",
                versionCode = build.versionCode,
                certFingerprint = build.certFingerprint,
                minSdk = build.minSdk,
            )
        } returns InstallStatus.NotInstalled
        every { downloadRepository.observeDownload(build.id) } returns flowOf(DownloadState.Idle)
        coEvery { repository.getDownloadUrl(build.id) } returns Result.Success("https://example.test/app.apk")

        viewModel.setLatestBuild(build, "Example", "ru.example.app")
        viewModel.onLatestInstallClick()
        advanceUntilIdle()

        verify(exactly = 1) {
            startDownload(
                match { it.id == build.id && it.packageName == "ru.example.app" },
                "https://example.test/app.apk",
            )
        }
    }

    private fun build() = BuildUi(
        id = "build-1",
        projectId = "proj-1",
        projectName = "",
        packageName = "",
        versionName = "1.2.3",
        versionCode = 123,
        channel = "internal",
        environment = "qa",
        buildType = "release",
        changelog = null,
        fileSize = 1024,
        checksumSha256 = "checksum",
        status = "active",
        isLatestInChannel = true,
        uploadDate = 0,
        uploaderName = "tester",
        minSdk = 24,
        targetSdk = 35,
        certFingerprint = null,
        abis = emptyList(),
        expiryDate = null,
        branch = null,
        commitHash = null,
    )
}
