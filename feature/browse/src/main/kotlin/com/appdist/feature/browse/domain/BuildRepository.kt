package com.appdist.feature.browse.domain

import androidx.paging.PagingData
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.browse.ui.builds.BuildFilters
import kotlinx.coroutines.flow.Flow

interface BuildRepository {
    fun getBuilds(projectId: String, filters: BuildFilters): Flow<PagingData<BuildUi>>
    suspend fun refreshLatestBuilds(projectIds: Collection<String>): Result<Unit>
    suspend fun getBuild(buildId: String): Result<BuildUi>
    suspend fun getDownloadUrl(buildId: String): Result<String>
    suspend fun getFilterOptions(projectId: String): Result<BuildFilterOptions>
}

/** Значения быстрых фильтров журнала, самые частые — первыми. */
data class BuildFilterOptions(
    val flavors: List<String> = emptyList(),
    val stands: List<String> = emptyList(),
)
