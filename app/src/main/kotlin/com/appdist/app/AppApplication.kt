package com.appdist.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.appdist.core.downloads.DownloadRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // dagger.Lazy, а не прямой DownloadRepository: тот тянет за собой
    // WorkManager.getInstance(context), а тот, если WorkManager ещё не
    // инициализирован, синхронно зовёт обратно workManagerConfiguration —
    // то есть читает workerFactory. Прямое поле требовало бы, чтобы Dagger
    // успел присвоить workerFactory раньше downloadRepository во время
    // member injection; порядок между двумя @Inject-полями ничем не
    // гарантирован, и его перестановка роняла бы холодный старт с
    // UninitializedPropertyAccessException. Lazy откладывает получение
    // WorkManager до вызова .get() ниже, когда workerFactory уже точно готов,
    // и заодно снимает синхронный WorkManager.getInstance() с main-потока.
    @Inject lateinit var downloadRepository: dagger.Lazy<DownloadRepository>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        appScope.launch {
            try {
                downloadRepository.get().reconcileStaleDownloads()
            } catch (e: Exception) {
                Timber.e(e, "Failed to reconcile stale downloads")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
