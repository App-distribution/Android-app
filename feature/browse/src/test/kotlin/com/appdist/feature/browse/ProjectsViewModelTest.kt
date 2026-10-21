package com.appdist.feature.browse

import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.feature.browse.domain.GetProjectsUseCase
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.domain.ProjectRepository
import com.appdist.feature.browse.ui.projects.ProjectsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ProjectRepository
    private lateinit var buildDao: BuildDao
    private lateinit var buildRepository: BuildRepository
    private val projects = MutableStateFlow<List<ProjectResponse>>(emptyList())

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        buildDao = mockk()
        buildRepository = mockk()
        every { repository.getProjects() } returns projects
        every { buildDao.getLatestBuildPerProject() } returns MutableStateFlow(emptyList())
        coEvery { buildRepository.refreshLatestBuilds(any()) } returns Result.Success(Unit)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `backend error is exposed with status code`() = runTest {
        coEvery { repository.refresh() } returns Result.Error(
            AppError.Network(503, "Unavailable")
        )

        val viewModel = ProjectsViewModel(GetProjectsUseCase(repository), buildRepository, buildDao)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(503, (state.error as AppError.Network).code)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry clears error after successful refresh`() = runTest {
        val project = ProjectResponse(
            id = "project-1",
            workspaceId = "workspace-1",
            name = "Mobile App",
            packageName = "com.example.mobile",
            createdAt = 0L,
        )
        coEvery { repository.refresh() } returnsMany listOf(
            Result.Error(AppError.Network(503, "Unavailable")),
            Result.Success(listOf(project)),
        )

        val viewModel = ProjectsViewModel(GetProjectsUseCase(repository), buildRepository, buildDao)
        viewModel.retry()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(listOf(project), state.projects)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 2) { repository.refresh() }
        coVerify(exactly = 1) { buildRepository.refreshLatestBuilds(listOf("project-1")) }
    }

    @Test
    fun `latest build refresh failure remains distinguishable from an empty project`() = runTest {
        val project = ProjectResponse(
            id = "project-1",
            workspaceId = "workspace-1",
            name = "Mobile App",
            packageName = "com.example.mobile",
            createdAt = 0L,
        )
        coEvery { repository.refresh() } returns Result.Success(listOf(project))
        coEvery { buildRepository.refreshLatestBuilds(any()) } returns Result.Error(
            AppError.Network(503, "Unavailable"),
        )

        val viewModel = ProjectsViewModel(GetProjectsUseCase(repository), buildRepository, buildDao)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLatestBuildsLoading)
            assertEquals(503, (state.latestBuildsError as AppError.Network).code)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
