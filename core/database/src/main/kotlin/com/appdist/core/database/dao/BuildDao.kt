package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.BuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildDao {
    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY uploadDate DESC")
    fun getBuildsForProject(projectId: String): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds ORDER BY uploadDate DESC LIMIT :limit")
    fun getRecentBuilds(limit: Int): Flow<List<BuildEntity>>

    /**
     * Последняя закэшированная сборка каждого проекта — журнал приложений
     * показывает её прямо в карточке, до открытия истории релизов.
     */
    @Query(
        """
        SELECT b.* FROM builds b
        JOIN (
            SELECT projectId, MAX(uploadDate) AS maxDate FROM builds GROUP BY projectId
        ) latest ON b.projectId = latest.projectId AND b.uploadDate = latest.maxDate
        GROUP BY b.projectId
        """
    )
    fun getLatestBuildPerProject(): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds WHERE id = :id")
    suspend fun getBuildById(id: String): BuildEntity?

    @Upsert
    suspend fun upsertBuilds(builds: List<BuildEntity>)

    @Query("DELETE FROM builds WHERE cachedAt < :olderThan")
    suspend fun deleteStale(olderThan: Long)
}
