package com.appdist.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadsMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migration keeps rows, adds columns and normalizes completed state`() = runTest {
        createV1DatabaseWithRows()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()

        val ready = db.downloadDao().observeReadyDownloads().first()
        assertEquals(1, ready.size)
        assertEquals("done", ready[0].buildId)
        assertEquals("/data/done.apk", ready[0].localPath)
        assertEquals("ready", ready[0].state)
        // completedAt подставлен из startedAt
        assertEquals(1000L, ready[0].completedAt)
        // новые колонки получили значения по умолчанию
        assertEquals("", ready[0].versionName)
        assertEquals(0L, ready[0].versionCode)
        assertEquals("", ready[0].channel)

        // completed без файла считается неудачей и в «Скачанные» не попадает
        val orphan = db.downloadDao().getByBuildId("orphan")
        assertEquals("failed", orphan?.state)
        assertNull(orphan?.localPath)

        db.close()
    }

    private fun createV1DatabaseWithRows() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `downloads` (" +
                        "`buildId` TEXT NOT NULL, `localPath` TEXT, `state` TEXT NOT NULL, " +
                        "`progress` REAL NOT NULL, `bytesLoaded` INTEGER NOT NULL, " +
                        "`totalBytes` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`buildId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `builds` (" +
                        "`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `versionName` TEXT NOT NULL, " +
                        "`versionCode` INTEGER NOT NULL, `channel` TEXT NOT NULL, `environment` TEXT NOT NULL, " +
                        "`changelog` TEXT, `fileSize` INTEGER NOT NULL, `checksumSha256` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `isLatestInChannel` INTEGER NOT NULL, " +
                        "`uploadDate` INTEGER NOT NULL, `uploaderName` TEXT NOT NULL, " +
                        "`cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_builds_projectId` ON `builds` (`projectId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `install_history` (" +
                        "`id` TEXT NOT NULL, `buildId` TEXT NOT NULL, `packageName` TEXT NOT NULL, " +
                        "`versionName` TEXT NOT NULL, `versionCode` INTEGER NOT NULL, " +
                        "`installedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )

        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO downloads VALUES ('done', '/data/done.apk', 'completed', 1.0, 100, 100, 1000)"
            )
            db.execSQL(
                "INSERT INTO downloads VALUES ('orphan', NULL, 'completed', 1.0, 100, 100, 2000)"
            )
        }
    }
}
