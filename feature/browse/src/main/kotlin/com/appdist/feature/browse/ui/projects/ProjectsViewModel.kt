package com.appdist.feature.browse.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.feature.browse.domain.GetProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<ProjectResponse> = emptyList(),
    val latestBuilds: Map<String, BuildEntity> = emptyMap(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjects: GetProjectsUseCase,
    buildDao: BuildDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            getProjects().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
        viewModelScope.launch {
            buildDao.getLatestBuildPerProject().collect { builds ->
                _state.update { it.copy(latestBuilds = builds.associateBy { build -> build.projectId }) }
            }
        }
        refresh()
    }

    fun retry() = refresh()

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getProjects.refresh()) {
                is Result.Success -> _state.update {
                    it.copy(projects = result.data, isLoading = false, error = null)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}
