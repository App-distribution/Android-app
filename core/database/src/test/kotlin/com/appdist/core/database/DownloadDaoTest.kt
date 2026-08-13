package com.appdist.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.downloadDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `active downloads exclude ready ones`() = runTest {
        dao.upsert(entity("running", state = "downloading", localPath = null))
        dao.upsert(entity("checking", state = "verifying", localPath = null))
        dao.upsert(entity("done", state = "ready", localPath = "/data/done.apk"))

        dao.getActiveDownloads().test {
            val items = awaitItem()
            assertEquals(setOf("running", "checking"), items.map { it.buildId }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ready downloads include legacy completed state`() = runTest {
        dao.upsert(entity("new", state = "ready", localPath = "/data/new.apk"))
        dao.upsert(entity("legacy", state = "completed", localPath = "/data/legacy.apk"))
        dao.upsert(entity("running", state = "downloading", localPath = null))

        dao.observeReadyDownloads().test {
            val items = awaitItem()
            assertEquals(setOf("new", "legacy"), items.map { it.buildId }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ready downloads skip rows without local path`() = runTest {
        dao.upsert(entity("nopath", state = "ready", localPath = null))

        dao.observeReadyDownloads().test {
            assertEquals(emptyList<DownloadEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun entity(
        buildId: String,
        state: String,
        localPath: String?
    ) = DownloadEntity(
        buildId = buildId,
        localPath = localPath,
        state = state,
        progress = 1f,
        bytesLoaded = 100L,
        totalBytes = 100L,
        startedAt = 1000L,
        versionName = "1.0",
        versionCode = 1L,
        channel = "alpha",
        completedAt = 2000L
    )
}
