package com.appdist.feature.browse

import com.appdist.core.common.Result
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.BuildResponse
import com.appdist.feature.browse.data.BuildRepositoryImpl
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Response

class BuildRepositoryImplTest {

    private val api = mockk<ApiService>()
    private val buildDao = mockk<BuildDao>()
    private val repository = BuildRepositoryImpl(api, buildDao)

    @Test
    fun `refresh latest builds caches the first build for every project`() = runTest {
        val build = build(projectId = "project-1")
        coEvery { api.getBuilds("project-1", null, null, null, 0, 1) } returns
            Response.success(listOf(build))
        coJustRun { buildDao.upsertBuilds(any()) }

        val result = repository.refreshLatestBuilds(listOf("project-1"))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            buildDao.upsertBuilds(match { cached ->
                cached.single().let {
                    it.id == build.id &&
                        it.projectId == build.projectId &&
                        it.versionCode == build.versionCode
                }
            })
        }
    }

    @Test
    fun `refresh latest builds reports a failed project request`() = runTest {
        coEvery { api.getBuilds("project-1", null, null, null, 0, 1) } returns
            Response.error(503, "Unavailable".toResponseBody())

        val result = repository.refreshLatestBuilds(listOf("project-1"))

        assertTrue(result is Result.Error)
        assertEquals(503, (result as Result.Error).error.let { error ->
            (error as com.appdist.core.common.AppError.Network).code
        })
        coVerify(exactly = 0) { buildDao.upsertBuilds(any()) }
    }

    private fun build(projectId: String) = BuildResponse(
        id = "build-1",
        projectId = projectId,
        versionName = "1.2.3",
        versionCode = 123,
        buildType = "release",
        environment = "qa",
        channel = "internal",
        uploaderName = "tester",
        uploadDate = 1_000,
        fileSize = 1_024,
        checksumSha256 = "checksum",
        minSdk = 24,
        targetSdk = 35,
        status = "active",
        isLatestInChannel = true,
    )
}
