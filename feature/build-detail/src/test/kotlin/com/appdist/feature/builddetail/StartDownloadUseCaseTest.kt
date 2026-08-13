package com.appdist.feature.builddetail

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.appdist.core.common.model.BuildUi
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.feature.builddetail.domain.StartDownloadUseCase
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartDownloadUseCaseTest {

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val useCase = StartDownloadUseCase(workManager)

    private val fakeBuild = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0", versionCode = 1L, channel = "alpha", environment = "dev",
        buildType = "debug", changelog = null, fileSize = 1024L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", minSdk = 24, targetSdk = 35,
        certFingerprint = null, abis = emptyList(), expiryDate = null,
        branch = null, commitHash = null
    )

    /**
     * Регрессия: имя уникальной работы, под которым StartDownloadUseCase ставит
     * загрузку в очередь, и имя, по которому DownloadRepository.observeDownload
     * её ищет, обязаны совпадать — расхождение здесь и есть баг, ради
     * исправления которого существует эта ветка (KEEP тихо отбрасывает второй
     * запрос, а подписка ищет не тот UUID).
     */
    @Test
    fun `enqueues work under the name the shared helper produces for the same build id`() {
        val nameSlot = slot<String>()

        useCase(fakeBuild, "https://cdn/b1.apk")

        verify {
            workManager.enqueueUniqueWork(
                capture(nameSlot),
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }
        assertEquals(DownloadWorkKeys.uniqueWorkName(fakeBuild.id), nameSlot.captured)
        assertEquals("download_b1", nameSlot.captured)
    }
}
