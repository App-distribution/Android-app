package com.appdist.feature.builddetail.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.toAppError
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadState
import com.appdist.feature.builddetail.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class BuildDetailUiState(
    val build: BuildUi? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

sealed interface BuildDetailAction {
    data object DownloadOrInstallClicked : BuildDetailAction
    data object CancelDownload : BuildDetailAction
    data object CopyLinkClicked : BuildDetailAction
    data object RetryClicked : BuildDetailAction
}

sealed interface BuildDetailEffect {
    data class LaunchInstaller(val apkUri: Uri) : BuildDetailEffect
    data class ShowSnackbar(val message: String) : BuildDetailEffect
    data class CopyToClipboard(val text: String) : BuildDetailEffect
    data class ShowSignatureMismatchDialog(val installedFp: String) : BuildDetailEffect
    data class ShowDowngradeWarning(val installedCode: Long, val newCode: Long) : BuildDetailEffect
}

@HiltViewModel
class BuildDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getBuildDetail: GetBuildDetailUseCase,
    private val getInstallStatus: GetInstallStatusUseCase,
    private val startDownload: StartDownloadUseCase,
    private val downloadRepository: DownloadRepository,
    private val reportInstall: ReportInstallUseCase
) : ViewModel() {

    val buildId: String = checkNotNull(savedStateHandle["buildId"])

    private val _state = MutableStateFlow(BuildDetailUiState())
    val state: StateFlow<BuildDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<BuildDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Установщик запускается автоматически только для загрузки, начатой в этой
     * сессии экрана. Без флага восстановленное состояние ReadyToInstall
     * открывало бы системный установщик при каждом заходе на экран.
     */
    private var autoInstallRequested = false

    init {
        loadBuild()
        observeDownloadState()
    }

    fun onAction(action: BuildDetailAction) = when (action) {
        BuildDetailAction.DownloadOrInstallClicked -> handleInstallAction()
        BuildDetailAction.CancelDownload -> cancelDownload()
        BuildDetailAction.CopyLinkClicked -> copyLink()
        BuildDetailAction.RetryClicked -> loadBuild()
    }

    /**
     * Подписка живёт в init, поэтому переживает пересоздание ViewModel при
     * возврате на экран: состояние идущей загрузки восстанавливается из
     * WorkManager, а не начинается с Idle.
     */
    private fun observeDownloadState() {
        viewModelScope.launch {
            downloadRepository.observeDownload(buildId)
                .catch { e ->
                    // combine() из Room-flow и WorkManager-flow может упасть (диск
                    // полон, база повреждена, сбой в БД самого WorkManager). Без
                    // этого исключение ушло бы необработанным из viewModelScope и
                    // уронило бы процесс на главном экране приложения.
                    Timber.e(e, "Failed to observe download state for build %s", buildId)
                    _state.update { it.copy(downloadState = DownloadState.Failed(e.toAppError())) }
                }
                .collect { downloadState ->
                    _state.update { it.copy(downloadState = downloadState) }

                    if (downloadState is DownloadState.ReadyToInstall && autoInstallRequested) {
                        autoInstallRequested = false
                        launchInstaller(downloadState.filePath)
                    }
                    if (downloadState is DownloadState.Failed && autoInstallRequested) {
                        autoInstallRequested = false
                        _effects.send(BuildDetailEffect.ShowSnackbar("Ошибка загрузки"))
                    }
                }
        }
    }

    private fun loadBuild() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getBuildDetail(buildId)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.toAppError()) }
                }
                .collect { build ->
                    val status = getInstallStatus(build.packageName, build.versionCode, build.certFingerprint, build.minSdk)
                    _state.update { it.copy(build = build.copy(installStatus = status), isLoading = false) }
                }
        }
    }

    private fun handleInstallAction() {
        val build = _state.value.build ?: return
        val downloadState = _state.value.downloadState

        if (downloadState is DownloadState.ReadyToInstall) {
            launchInstaller(downloadState.filePath)
            return
        }

        val status = build.installStatus
        if (status is InstallStatus.SignatureMismatch) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowSignatureMismatchDialog(status.installedFingerprint))
            }
            return
        }

        if (status is InstallStatus.InstalledNewer) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowDowngradeWarning(status.installed, status.available))
            }
            return
        }

        enqueueDownload(build)
    }

    private fun enqueueDownload(build: BuildUi) {
        viewModelScope.launch {
            val urlResult = getBuildDetail.getDownloadUrl(buildId)
            if (urlResult is Result.Error) {
                _effects.send(BuildDetailEffect.ShowSnackbar("Не удалось получить ссылку на скачивание"))
                return@launch
            }
            autoInstallRequested = true
            startDownload(build, (urlResult as Result.Success).data)
        }
    }

    private fun launchInstaller(filePath: String) {
        viewModelScope.launch {
            reportInstall(buildId)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(filePath)
            )
            _effects.send(BuildDetailEffect.LaunchInstaller(apkUri))
        }
    }

    private fun cancelDownload() {
        viewModelScope.launch {
            autoInstallRequested = false
            downloadRepository.cancel(buildId)
        }
    }

    private fun copyLink() {
        viewModelScope.launch {
            _effects.send(BuildDetailEffect.CopyToClipboard("appdist://builds/$buildId"))
        }
    }
}
