package com.appdist.feature.browse.data

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.toAppError
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.feature.browse.domain.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ProjectRepository {

    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())

    override fun getProjects(): Flow<List<ProjectResponse>> = _projects.asStateFlow()

    override suspend fun refresh(): Result<List<ProjectResponse>> = try {
        val response = api.getProjects()
        if (response.isSuccessful) {
            val projects = response.body().orEmpty()
            _projects.value = projects
            Result.Success(projects)
        } else {
            Result.Error(AppError.Network(response.code(), response.message()))
        }
    } catch (error: Exception) {
        Result.Error(error.toAppError())
    }
}
