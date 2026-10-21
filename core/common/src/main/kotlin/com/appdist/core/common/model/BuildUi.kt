package com.appdist.core.common.model

data class BuildUi(
    val id: String,
    val projectId: String,
    val projectName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val environment: String,
    val buildType: String,
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val status: String,
    val isLatestInChannel: Boolean,
    val uploadDate: Long,
    val uploaderName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val expiryDate: Long?,
    val branch: String?,
    val commitHash: String?,
    val installStatus: InstallStatus = InstallStatus.NotInstalled,
    /** Вид сборки: prod, stage, dev, stage-rustore… */
    val flavor: String? = null,
    /** Стенд, на который смотрит сборка: l10, dev3, prod… У старых сборок — null. */
    val stand: String? = null,
) {
    /**
     * Короткая подпись «вид · стенд». У старых сборок, загруженных до
     * появления стенда, — прежнее окружение.
     */
    val tagLabel: String
        get() = listOfNotNull(flavor, stand).joinToString(" · ").ifBlank { environment.lowercase() }
}

sealed interface InstallStatus {
    data object NotInstalled : InstallStatus
    data class Installed(val versionCode: Long) : InstallStatus
    data class UpdateAvailable(val installed: Long, val available: Long) : InstallStatus
    data class InstalledNewer(val installed: Long, val available: Long) : InstallStatus
    data class SignatureMismatch(val installedFingerprint: String) : InstallStatus
    data object Incompatible : InstallStatus
}
