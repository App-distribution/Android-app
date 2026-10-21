package com.appdist.feature.browse.ui.builds

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.common.toAppError
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadState
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.domain.GetBuildsUseCase
import com.appdist.feature.builddetail.domain.GetInstallStatusUseCase
import com.appdist.feature.builddetail.domain.ReportInstallUseCase
import com.appdist.feature.builddetail.domain.StartDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class LatestBuildInstallState(
    val build: BuildUi? = null,
    val downloadState: DownloadState = DownloadState.Idle,
)

sealed interface BuildsEffect {
    data class LaunchInstaller(val apkUri: Uri) : BuildsEffect
    data class ShowSnackbar(val message: String) : BuildsEffect
    data class ShowSignatureMismatchDialog(val installedFp: String) : BuildsEffect
    data class ShowDowngradeWarning(val installedCode: Long, val newCode: Long) : BuildsEffect
}

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val getBuilds: GetBuildsUseCase,
    private val buildRepository: BuildRepository,
    @ApplicationContext private val context: Context,
    private val getInstallStatus: GetInstallStatusUseCase,
    private val startDownload: StartDownloadUseCase,
    private val downloadRepository: DownloadRepository,
    private val reportInstall: ReportInstallUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _filters = MutableStateFlow(BuildFilters())
    val filters: StateFlow<BuildFilters> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val builds = _filters.flatMapLatest { filters ->
        getBuilds(projectId, filters)
    }.cachedIn(viewModelScope)

    private val _latestInstallState = MutableStateFlow(LatestBuildInstallState())
    val latestInstallState: StateFlow<LatestBuildInstallState> = _latestInstallState.asStateFlow()

    private val _effects = Channel<BuildsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var downloadObservation: Job? = null
    private var autoInstallRequested = false

    fun setChannel(channel: String?) = _filters.update { it.copy(channel = channel) }
    fun setEnvironment(env: String?) = _filters.update { it.copy(environment = env) }
    fun setSearch(query: String) = _filters.update { it.copy(searchQuery = query) }
    fun clearFilters() = _filters.update { BuildFilters() }

    fun setLatestBuild(build: BuildUi, projectName: String, packageName: String) {
        val prepared = build.copy(
            projectName = projectName,
            packageName = packageName,
            installStatus = getInstallStatus(
                packageName = packageName,
                versionCode = build.versionCode,
                certFingerprint = build.certFingerprint,
                minSdk = build.minSdk,
            ),
        )

        if (_latestInstallState.value.build?.id == prepared.id) {
            _latestInstallState.update { it.copy(build = prepared) }
            return
        }

        autoInstallRequested = false
        downloadObservation?.cancel()
        _latestInstallState.value = LatestBuildInstallState(build = prepared)
        downloadObservation = viewModelScope.launch {
            downloadRepository.observeDownload(prepared.id)
                .catch { error ->
                    Timber.e(error, "Failed to observe latest build download %s", prepared.id)
                    _latestInstallState.update {
                        it.copy(downloadState = DownloadState.Failed(error.toAppError()))
                    }
                    if (autoInstallRequested) {
                        autoInstallRequested = false
                        _effects.send(BuildsEffect.ShowSnackbar("Ошибка загрузки"))
                    }
                }
                .collect { downloadState ->
                    _latestInstallState.update { it.copy(downloadState = downloadState) }
                    if (downloadState is DownloadState.ReadyToInstall && autoInstallRequested) {
                        autoInstallRequested = false
                        launchInstaller(prepared.id, downloadState.filePath)
                    }
                    if (downloadState is DownloadState.Failed && autoInstallRequested) {
                        autoInstallRequested = false
                        _effects.send(BuildsEffect.ShowSnackbar("Ошибка загрузки"))
                    }
                }
        }
    }

    fun onLatestInstallClick() {
        val state = _latestInstallState.value
        val build = state.build ?: return

        when (val downloadState = state.downloadState) {
            is DownloadState.Downloading -> cancelLatestDownload()
            DownloadState.Verifying -> Unit
            is DownloadState.ReadyToInstall -> launchInstaller(build.id, downloadState.filePath)
            else -> when (val status = build.installStatus) {
                is InstallStatus.SignatureMismatch -> viewModelScope.launch {
                    _effects.send(BuildsEffect.ShowSignatureMismatchDialog(status.installedFingerprint))
                }
                is InstallStatus.InstalledNewer -> viewModelScope.launch {
                    _effects.send(BuildsEffect.ShowDowngradeWarning(status.installed, status.available))
                }
                InstallStatus.Incompatible, is InstallStatus.Installed -> Unit
                else -> enqueueDownload(build)
            }
        }
    }

    private fun enqueueDownload(build: BuildUi) {
        viewModelScope.launch {
            when (val urlResult = buildRepository.getDownloadUrl(build.id)) {
                is Result.Error -> _effects.send(
                    BuildsEffect.ShowSnackbar("Не удалось получить ссылку на скачивание")
                )
                is Result.Success -> {
                    autoInstallRequested = true
                    runCatching { startDownload(build, urlResult.data) }
                        .onFailure { error ->
                            autoInstallRequested = false
                            Timber.e(error, "Failed to enqueue latest build download %s", build.id)
                            _effects.send(BuildsEffect.ShowSnackbar("Не удалось начать загрузку"))
                        }
                }
            }
        }
    }

    private fun cancelLatestDownload() {
        val buildId = _latestInstallState.value.build?.id ?: return
        viewModelScope.launch {
            autoInstallRequested = false
            downloadRepository.cancel(buildId)
        }
    }

    private fun launchInstaller(buildId: String, filePath: String) {
        viewModelScope.launch {
            reportInstall(buildId)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(filePath),
            )
            _effects.send(BuildsEffect.LaunchInstaller(apkUri))
        }
    }
}
