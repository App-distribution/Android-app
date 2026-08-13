package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    /** Идущие загрузки — для раздела «Загрузки». 'completed' сюда больше не попадает. */
    @Query("SELECT * FROM downloads WHERE state IN ('downloading', 'verifying')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    /** Скачанные файлы. 'completed' — легаси-состояние от версий до миграции 1 → 2. */
    @Query(
        "SELECT * FROM downloads WHERE state IN ('ready', 'completed') AND localPath IS NOT NULL " +
            "ORDER BY completedAt DESC, startedAt DESC"
    )
    fun observeReadyDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE buildId = :buildId")
    fun observeByBuildId(buildId: String): Flow<DownloadEntity?>

    @Query(
        "SELECT * FROM downloads WHERE state IN ('ready', 'completed') AND localPath IS NOT NULL"
    )
    suspend fun getReadyDownloads(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE state IN ('downloading', 'verifying')")
    suspend fun getStuckDownloads(): List<DownloadEntity>

    @Query("UPDATE downloads SET state = 'failed' WHERE buildId = :buildId")
    suspend fun markFailed(buildId: String)

    @Query("SELECT * FROM downloads WHERE buildId = :buildId")
    suspend fun getByBuildId(buildId: String): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE buildId = :buildId")
    suspend fun delete(buildId: String)
}
