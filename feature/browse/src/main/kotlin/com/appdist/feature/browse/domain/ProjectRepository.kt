package com.appdist.feature.browse.domain

import com.appdist.core.common.Result
import com.appdist.core.network.dto.ProjectResponse
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<ProjectResponse>>
    suspend fun refresh(): Result<List<ProjectResponse>>
}
