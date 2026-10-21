package com.appdist.feature.browse.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.toAppError
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.BuildResponse
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.ui.builds.BuildFilters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val buildDao: BuildDao,
) : BuildRepository {

    override fun getBuilds(projectId: String, filters: BuildFilters): Flow<PagingData<BuildUi>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { BuildsPagingSource(api, buildDao, projectId, filters) }
        ).flow

    override suspend fun refreshLatestBuilds(projectIds: Collection<String>): Result<Unit> =
        coroutineScope {
            val results = projectIds.distinct().map { projectId ->
                async {
                    try {
                        val response = api.getBuilds(projectId = projectId, page = 0, limit = 1)
                        if (response.isSuccessful) {
                            LatestBuildFetch(build = response.body()?.firstOrNull())
                        } else {
                            LatestBuildFetch(
                                error = AppError.Network(response.code(), response.message()),
                            )
                        }
                    } catch (error: Exception) {
                        LatestBuildFetch(error = error.toAppError())
                    }
                }
            }.awaitAll()

            val builds = results.mapNotNull { it.build?.toEntity() }
            if (builds.isNotEmpty()) buildDao.upsertBuilds(builds)

            results.firstNotNullOfOrNull { it.error }
                ?.let { Result.Error(it) }
                ?: Result.Success(Unit)
        }

    override suspend fun getBuild(buildId: String): Result<BuildUi> = try {
        val response = api.getBuild(buildId)
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            Result.Success(body.toUi())
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }

    override suspend fun getDownloadUrl(buildId: String): Result<String> = try {
        val response = api.getDownloadUrl(buildId)
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            Result.Success(body.url)
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }

}

private data class LatestBuildFetch(
    val build: BuildResponse? = null,
    val error: AppError? = null,
)
