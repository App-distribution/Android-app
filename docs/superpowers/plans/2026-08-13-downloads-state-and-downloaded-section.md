# Восстановление состояния загрузки и раздел «Скачанные» — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Починить пропажу индикатора загрузки и «мёртвую» кнопку при возврате на экран сборки, и добавить в «Мои сборки» раздел «Скачанные» с ручным удалением файлов.

**Architecture:** Состояние загрузки наблюдается по **имени уникальной работы** WorkManager (`getWorkInfosForUniqueWorkFlow`), а не по UUID запроса, и подписка живёт в `init` ViewModel — поэтому переживает пересоздание. Room-таблица `downloads` становится источником правды о том, какие APK лежат на диске. Общая логика вынесена в новый модуль `:core:downloads`; `DownloadWorker` при этом **остаётся на месте** ради обратной совместимости.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room 2.7.0, WorkManager 2.10.0, Coroutines/Flow. Тесты: JUnit5 + MockK + Turbine в feature-модулях и `:core:downloads`; JUnit4 + Robolectric в `:core:database`.

## Global Constraints

Эти инварианты нарушать нельзя ни в одной задаче. Значения скопированы из спеки дословно.

- Имя уникальной работы: `download_$buildId` — не менять.
- FQN воркера: `com.appdist.feature.builddetail.data.DownloadWorker` — класс не переносить в другой модуль и не переименовывать. WorkManager хранит имя класса в своей БД; смена FQN уронит работы, поставленные в очередь до обновления.
- Значения ключей `inputData`: `build_id`, `download_url`, `expected_checksum`, `file_size`, `output_path`, `progress`, `bytes_loaded` — строки не менять. Новые ключи только добавлять.
- Путь файла: `cacheDir/apk_downloads/<buildId>.apk` — не менять.
- Authority FileProvider `${packageName}.fileprovider` и содержимое `app/src/main/res/xml/file_paths.xml` — не менять.
- Имя БД `appdist.db`, имя таблицы `downloads`, первичный ключ `buildId` — не менять.
- Значение `state = 'completed'`, записанное старым воркером, читается как «готово к установке».
- Миграция БД 1 → 2 только через `ALTER TABLE`. `fallbackToDestructiveMigration()` остаётся в `DatabaseModule`, но не должен срабатывать на пути 1 → 2.
- Каждая задача завершается зелёной сборкой: `./gradlew assembleDebug` должен проходить после любой задачи.

---

### Task 1: Экспорт схемы БД версии 1

Схему выпущенной версии надо зафиксировать **до** изменения сущностей — иначе она будет соответствовать новой схеме и потеряет смысл как история.

**Files:**
- Modify: `core/database/build.gradle.kts`
- Modify: `core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt:15`
- Create: `core/database/schemas/com.appdist.core.database.AppDatabase/1.json` (генерируется сборкой)

**Interfaces:**
- Consumes: ничего
- Produces: файл `1.json` с точным DDL таблиц версии 1. Task 2 берёт из него DDL для теста миграции.

- [ ] **Step 1: Включить экспорт схемы в build.gradle.kts**

В `core/database/build.gradle.kts` добавить блок `ksp` на верхнем уровне файла (после блока `android { ... }`):

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

- [ ] **Step 2: Включить exportSchema в AppDatabase**

В `core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt` заменить `exportSchema = false` на `exportSchema = true`:

```kotlin
@Database(
    entities = [BuildEntity::class, InstallHistoryEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buildDao(): BuildDao
    abstract fun installHistoryDao(): InstallHistoryDao
    abstract fun downloadDao(): DownloadDao
}
```

- [ ] **Step 3: Собрать модуль, чтобы сгенерировать схему**

Run: `./gradlew :core:database:kspDebugKotlin`
Expected: BUILD SUCCESSFUL, появился файл `core/database/schemas/com.appdist.core.database.AppDatabase/1.json`

- [ ] **Step 4: Проверить, что схема сгенерирована и содержит таблицу downloads**

Run: `grep -c '"tableName": "downloads"' core/database/schemas/com.appdist.core.database.AppDatabase/1.json`
Expected: `1`

- [ ] **Step 5: Commit**

```bash
git add core/database/build.gradle.kts core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt core/database/schemas
git commit -m "chore(database): включить экспорт схемы Room и зафиксировать схему версии 1"
```

---

### Task 2: Миграция БД 1 → 2, новые поля и запросы DAO

**Files:**
- Modify: `core/database/src/main/kotlin/com/appdist/core/database/entity/DownloadEntity.kt`
- Modify: `core/database/src/main/kotlin/com/appdist/core/database/dao/DownloadDao.kt`
- Modify: `core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt`
- Modify: `core/database/src/main/kotlin/com/appdist/core/database/DatabaseModule.kt`
- Test: `core/database/src/test/kotlin/com/appdist/core/database/DownloadsMigrationTest.kt` (create)
- Test: `core/database/src/test/kotlin/com/appdist/core/database/DownloadDaoTest.kt` (create)

**Interfaces:**
- Consumes: `1.json` из Task 1
- Produces:
  - `DownloadEntity(buildId: String, localPath: String?, state: String, progress: Float, bytesLoaded: Long, totalBytes: Long, startedAt: Long, versionName: String = "", versionCode: Long = 0L, channel: String = "", completedAt: Long? = null)`
  - `DownloadDao.getActiveDownloads(): Flow<List<DownloadEntity>>`
  - `DownloadDao.observeReadyDownloads(): Flow<List<DownloadEntity>>`
  - `DownloadDao.observeByBuildId(buildId: String): Flow<DownloadEntity?>`
  - `DownloadDao.getReadyDownloads(): List<DownloadEntity>`
  - `DownloadDao.getStuckDownloads(): List<DownloadEntity>`
  - `DownloadDao.markFailed(buildId: String)`
  - `com.appdist.core.database.MIGRATION_1_2: Migration`

- [ ] **Step 1: Написать падающий тест миграции**

Создать `core/database/src/test/kotlin/com/appdist/core/database/DownloadsMigrationTest.kt`.

DDL версии 1 ниже выписан из сущностей; **сверьте его с `core/database/schemas/com.appdist.core.database.AppDatabase/1.json`** из Task 1 — там лежит эталон, поле `createSql` каждой таблицы.

```kotlin
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
```

- [ ] **Step 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*DownloadsMigrationTest*"`
Expected: FAIL с ошибкой компиляции — `MIGRATION_1_2` и `observeReadyDownloads` не существуют

- [ ] **Step 3: Добавить поля в DownloadEntity**

Заменить содержимое `core/database/src/main/kotlin/com/appdist/core/database/entity/DownloadEntity.kt`:

```kotlin
package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val buildId: String,
    val localPath: String?,
    val state: String,   // downloading / verifying / ready / failed ('completed' — легаси до версии 2)
    val progress: Float,
    val bytesLoaded: Long,
    val totalBytes: Long,
    val startedAt: Long,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val channel: String = "",
    val completedAt: Long? = null
)
```

- [ ] **Step 4: Обновить запросы DownloadDao**

Заменить содержимое `core/database/src/main/kotlin/com/appdist/core/database/dao/DownloadDao.kt`:

```kotlin
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
```

- [ ] **Step 5: Поднять версию БД и объявить миграцию**

Заменить содержимое `core/database/src/main/kotlin/com/appdist/core/database/AppDatabase.kt`:

```kotlin
package com.appdist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity

@Database(
    entities = [BuildEntity::class, InstallHistoryEntity::class, DownloadEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buildDao(): BuildDao
    abstract fun installHistoryDao(): InstallHistoryDao
    abstract fun downloadDao(): DownloadDao
}

/**
 * Добавляет метаданные сборки в строки загрузок и приводит состояния к единому набору.
 * Разрушительная миграция здесь недопустима: она стёрла бы историю установок
 * у пользователей, уже обновляющихся с версии 1.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN versionName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE downloads ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloads ADD COLUMN channel TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE downloads ADD COLUMN completedAt INTEGER")
        db.execSQL(
            "UPDATE downloads SET state = 'ready', completedAt = startedAt " +
                "WHERE state = 'completed' AND localPath IS NOT NULL"
        )
        db.execSQL(
            "UPDATE downloads SET state = 'failed' WHERE state = 'completed' AND localPath IS NULL"
        )
    }
}
```

- [ ] **Step 6: Подключить миграцию в DatabaseModule**

В `core/database/src/main/kotlin/com/appdist/core/database/DatabaseModule.kt` заменить тело `provideDatabase`:

```kotlin
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "appdist.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
```

`fallbackToDestructiveMigration()` остаётся страховкой от падения на неожиданной схеме, но путь 1 → 2 теперь закрыт явной миграцией.

- [ ] **Step 7: Запустить тест миграции**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*DownloadsMigrationTest*"`
Expected: PASS

Если тест падает на `Room cannot verify the data integrity` — значит DDL версии 1 в тесте разошёлся с `1.json`. Сверьте поле `createSql` из `1.json` посимвольно.

- [ ] **Step 8: Написать тест DownloadDao на разделение active/ready**

Создать `core/database/src/test/kotlin/com/appdist/core/database/DownloadDaoTest.kt`:

```kotlin
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
```

- [ ] **Step 9: Запустить тесты модуля**

Run: `./gradlew :core:database:testDebugUnitTest`
Expected: PASS, включая существующий `BuildDaoTest`

- [ ] **Step 10: Проверить, что проект собирается**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add core/database
git commit -m "feat(database): миграция 1 -> 2 с метаданными сборки и единым набором состояний загрузки"
```

---

### Task 3: Модуль :core:downloads с DownloadState и DownloadRepository

**Files:**
- Create: `core/downloads/build.gradle.kts`
- Create: `core/downloads/src/main/AndroidManifest.xml`
- Create: `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadState.kt`
- Create: `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadWorkKeys.kt`
- Create: `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadPaths.kt`
- Create: `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadRepository.kt`
- Modify: `settings.gradle.kts`
- Modify: `feature/build-detail/build.gradle.kts`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/DownloadBuildUseCase.kt`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt:15`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailScreen.kt:23`
- Modify: `app/src/main/kotlin/com/appdist/app/AppApplication.kt`
- Modify: `app/build.gradle.kts`
- Test: `core/downloads/src/test/kotlin/com/appdist/core/downloads/DownloadRepositoryTest.kt`

**Interfaces:**
- Consumes: `DownloadDao` из Task 2 (`observeByBuildId`, `observeReadyDownloads`, `getReadyDownloads`, `getStuckDownloads`, `markFailed`, `delete`)
- Produces:
  - `com.appdist.core.downloads.DownloadState` — `Idle`, `Downloading(progress: Float, bytesLoaded: Long)`, `Verifying`, `ReadyToInstall(filePath: String)`, `Failed(reason: AppError)`
  - `com.appdist.core.downloads.DownloadedApk(buildId: String, filePath: String, sizeBytes: Long, versionName: String, versionCode: Long, channel: String, completedAt: Long)`
  - `DownloadWorkKeys` — константы ключей и состояний
  - `DownloadPaths.apkFile(context: Context, buildId: String): File`
  - `DownloadRepository.uniqueWorkName(buildId: String): String`
  - `DownloadRepository.observeDownload(buildId: String): Flow<DownloadState>`
  - `DownloadRepository.observeDownloaded(): Flow<List<DownloadedApk>>`
  - `DownloadRepository.cancel(buildId: String)` — suspend
  - `DownloadRepository.delete(buildId: String)` — suspend
  - `DownloadRepository.deleteAll()` — suspend
  - `DownloadRepository.reconcileStaleDownloads()` — suspend

- [ ] **Step 1: Зарегистрировать модуль в settings.gradle.kts**

В `settings.gradle.kts` добавить строку после `include(":core:database")`:

```kotlin
include(":core:downloads")
```

- [ ] **Step 2: Создать build.gradle.kts модуля**

Создать `core/downloads/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.appdist.core.downloads"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":core:common"))
    api(project(":core:database"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.workmanager.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
tasks.withType<Test> { useJUnitPlatform() }
```

Обе зависимости объявлены как `api`, а не `implementation`: `DownloadState.Failed` содержит `AppError` из `:core:common`, а `DownloadRepository` принимает `DownloadDao` из `:core:database`. При `implementation` эти типы не попали бы на compile-classpath потребителей.

- [ ] **Step 3: Создать манифест модуля**

Создать `core/downloads/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 4: Создать константы ключей и состояний**

Создать `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadWorkKeys.kt`:

```kotlin
package com.appdist.core.downloads

/**
 * Ключи inputData/progress воркера и значения состояний в таблице downloads.
 *
 * ВНИМАНИЕ: строковые значения существующих ключей менять нельзя — их читают
 * работы, поставленные в очередь предыдущими версиями приложения.
 */
object DownloadWorkKeys {
    const val KEY_BUILD_ID = "build_id"
    const val KEY_DOWNLOAD_URL = "download_url"
    const val KEY_EXPECTED_CHECKSUM = "expected_checksum"
    const val KEY_FILE_SIZE = "file_size"
    const val KEY_OUTPUT_PATH = "output_path"
    const val KEY_PROGRESS = "progress"
    const val KEY_BYTES_LOADED = "bytes_loaded"

    // Добавлены в версии 2 — у старых работ отсутствуют, читаются со значениями по умолчанию.
    const val KEY_VERSION_NAME = "version_name"
    const val KEY_VERSION_CODE = "version_code"
    const val KEY_CHANNEL = "channel"
    const val KEY_ERROR = "error"

    const val STATE_DOWNLOADING = "downloading"
    const val STATE_VERIFYING = "verifying"
    const val STATE_READY = "ready"
    const val STATE_FAILED = "failed"

    /** Записывалось воркером до миграции 1 → 2. Читается как «готово». */
    const val STATE_LEGACY_COMPLETED = "completed"
}
```

- [ ] **Step 5: Создать DownloadPaths**

Создать `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadPaths.kt`:

```kotlin
package com.appdist.core.downloads

import android.content.Context
import java.io.File

/**
 * Единственное место, задающее расположение скачанных APK.
 *
 * ВНИМАНИЕ: путь совпадает с объявленным в app/src/main/res/xml/file_paths.xml
 * (<cache-path name="apk_downloads" path="apk_downloads/" />). Менять нельзя —
 * сломается выдача URI установщику и потеряются уже скачанные файлы.
 */
object DownloadPaths {
    const val DIR_NAME = "apk_downloads"

    fun downloadsDir(context: Context): File = File(context.cacheDir, DIR_NAME)

    fun apkFile(context: Context, buildId: String): File = File(downloadsDir(context), "$buildId.apk")
}
```

- [ ] **Step 6: Создать DownloadState**

Создать `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadState.kt`:

```kotlin
package com.appdist.core.downloads

import com.appdist.core.common.AppError

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val bytesLoaded: Long) : DownloadState
    data object Verifying : DownloadState
    data class ReadyToInstall(val filePath: String) : DownloadState
    data class Failed(val reason: AppError) : DownloadState
}

/** Скачанный и проверенный APK, лежащий на диске. */
data class DownloadedApk(
    val buildId: String,
    val filePath: String,
    val sizeBytes: Long,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val completedAt: Long
)
```

- [ ] **Step 7: Написать падающий тест репозитория**

Создать `core/downloads/src/test/kotlin/com/appdist/core/downloads/DownloadRepositoryTest.kt`:

```kotlin
package com.appdist.core.downloads

import android.content.Context
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.cash.turbine.test
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryTest {

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var dao: DownloadDao
    private lateinit var workManager: WorkManager
    private lateinit var repository: DownloadRepository

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.cacheDir } returns cacheDir
        dao = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        repository = DownloadRepository(context, dao, workManager)
    }

    @Test
    fun `running work maps to Downloading with progress`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.RUNNING, workDataOf(
                DownloadWorkKeys.KEY_PROGRESS to 0.42f,
                DownloadWorkKeys.KEY_BYTES_LOADED to 4200L
            ))))
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_DOWNLOADING, localPath = null)
        )

        repository.observeDownload("b1").test {
            val state = awaitItem()
            assertTrue(state is DownloadState.Downloading)
            assertEquals(0.42f, (state as DownloadState.Downloading).progress)
            assertEquals(4200L, state.bytesLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enqueued work maps to Downloading at zero`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED, Data.EMPTY)))
        every { dao.observeByBuildId("b1") } returns flowOf(null)

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Downloading(0f, 0L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `existing file with no work maps to ReadyToInstall`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_READY, localPath = apk.absolutePath)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.ReadyToInstall(apk.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy completed state is treated as ready`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_LEGACY_COMPLETED, localPath = apk.absolutePath)
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.ReadyToInstall(apk.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ready row without file on disk maps to Idle`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns flowOf(emptyList())
        every { dao.observeByBuildId("b1") } returns flowOf(
            entity("b1", DownloadWorkKeys.STATE_READY, localPath = "/nope/gone.apk")
        )

        repository.observeDownload("b1").test {
            assertEquals(DownloadState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unfinished work wins over finished one`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("download_b1") } returns
            flowOf(listOf(
                workInfo(WorkInfo.State.FAILED, Data.EMPTY),
                workInfo(WorkInfo.State.RUNNING, workDataOf(
                    DownloadWorkKeys.KEY_PROGRESS to 0.1f,
                    DownloadWorkKeys.KEY_BYTES_LOADED to 10L
                ))
            ))
        every { dao.observeByBuildId("b1") } returns flowOf(null)

        repository.observeDownload("b1").test {
            assertTrue(awaitItem() is DownloadState.Downloading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes file and row`() = runTest {
        val apk = File(cacheDir, "apk_downloads/b1.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(10))
        }
        coEvery { dao.getByBuildId("b1") } returns entity(
            "b1", DownloadWorkKeys.STATE_READY, localPath = apk.absolutePath
        )

        repository.delete("b1")

        assertTrue(!apk.exists())
        coVerify { dao.delete("b1") }
    }

    @Test
    fun `observeDownloaded skips rows whose file is gone`() = runTest {
        val present = File(cacheDir, "apk_downloads/here.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(1024))
        }
        every { dao.observeReadyDownloads() } returns flowOf(
            listOf(
                entity("here", DownloadWorkKeys.STATE_READY, localPath = present.absolutePath),
                entity("gone", DownloadWorkKeys.STATE_READY, localPath = "/nope/gone.apk")
            )
        )

        repository.observeDownloaded().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("here", items[0].buildId)
            assertEquals(1024L, items[0].sizeBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reconcile marks stuck downloads as failed when no work is running`() = runTest {
        coEvery { dao.getStuckDownloads() } returns listOf(
            entity("stuck", DownloadWorkKeys.STATE_DOWNLOADING, localPath = null)
        )
        coEvery { dao.getReadyDownloads() } returns emptyList()
        every { workManager.getWorkInfosForUniqueWorkFlow("download_stuck") } returns flowOf(emptyList())

        repository.reconcileStaleDownloads()

        coVerify { dao.markFailed("stuck") }
    }

    private fun workInfo(state: WorkInfo.State, progress: Data) = WorkInfo(
        id = UUID.randomUUID(),
        state = state,
        tags = emptySet(),
        progress = progress
    )

    private fun entity(buildId: String, state: String, localPath: String?) = DownloadEntity(
        buildId = buildId,
        localPath = localPath,
        state = state,
        progress = 0f,
        bytesLoaded = 0L,
        totalBytes = 100L,
        startedAt = 1000L,
        versionName = "1.0",
        versionCode = 1L,
        channel = "alpha",
        completedAt = 2000L
    )
}
```

- [ ] **Step 8: Запустить тест и убедиться, что он падает**

Run: `./gradlew :core:downloads:testDebugUnitTest`
Expected: FAIL с ошибкой компиляции — `DownloadRepository` не существует

- [ ] **Step 9: Реализовать DownloadRepository**

Создать `core/downloads/src/main/kotlin/com/appdist/core/downloads/DownloadRepository.kt`:

```kotlin
package com.appdist.core.downloads

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.appdist.core.common.AppError
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager
) {

    /**
     * ВНИМАНИЕ: формат имени не менять — по нему находятся работы,
     * поставленные в очередь предыдущими версиями приложения.
     */
    fun uniqueWorkName(buildId: String): String = "download_$buildId"

    /**
     * Состояние загрузки конкретной сборки.
     *
     * WorkManager отвечает за то, что происходит сейчас, Room — за то, что лежит
     * на диске. Наблюдение идёт по имени уникальной работы, а не по UUID запроса:
     * при ExistingWorkPolicy.KEEP новый запрос отбрасывается и его UUID в базе
     * WorkManager не появляется.
     */
    fun observeDownload(buildId: String): Flow<DownloadState> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(buildId)),
        downloadDao.observeByBuildId(buildId)
    ) { infos, entity ->
        toDownloadState(infos, entity)
    }.flowOn(Dispatchers.IO)

    /** Скачанные и лежащие на диске APK — для раздела «Скачанные». */
    fun observeDownloaded(): Flow<List<DownloadedApk>> = downloadDao.observeReadyDownloads()
        .map { rows -> rows.mapNotNull { it.toDownloadedApk() } }
        .flowOn(Dispatchers.IO)

    /** Отмена загрузки: снимаем работу, удаляем частично скачанный файл и строку. */
    suspend fun cancel(buildId: String) = withContext(Dispatchers.IO) {
        workManager.cancelUniqueWork(uniqueWorkName(buildId))
        deleteFileAndRow(buildId)
    }

    suspend fun delete(buildId: String) = withContext(Dispatchers.IO) {
        deleteFileAndRow(buildId)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        downloadDao.getReadyDownloads().forEach { deleteFileAndRow(it.buildId) }
    }

    /**
     * Строка может навсегда остаться в состоянии downloading, если процесс воркера
     * был убит. Такие строки переводим в failed, иначе они вечно висят в разделе
     * «Загрузки». Заодно чистим строки, чей файл система удалила из кэша.
     */
    suspend fun reconcileStaleDownloads() = withContext(Dispatchers.IO) {
        downloadDao.getStuckDownloads().forEach { row ->
            // Flow-вариант, а не getWorkInfosForUniqueWork(): тот возвращает
            // ListenableFuture и потребовал бы guava в зависимостях.
            val infos = workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(row.buildId)).first()
            val stillRunning = infos.any { !it.state.isFinished }
            if (!stillRunning) downloadDao.markFailed(row.buildId)
        }
        downloadDao.getReadyDownloads().forEach { row ->
            if (row.localPath == null || !File(row.localPath).exists()) {
                downloadDao.delete(row.buildId)
            }
        }
    }

    private suspend fun deleteFileAndRow(buildId: String) {
        val path = downloadDao.getByBuildId(buildId)?.localPath
        if (path != null) File(path).delete()
        DownloadPaths.apkFile(context, buildId).delete()
        downloadDao.delete(buildId)
    }

    private fun toDownloadState(infos: List<WorkInfo>, entity: DownloadEntity?): DownloadState {
        val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
        val readyFile = entity
            ?.takeIf { it.state == DownloadWorkKeys.STATE_READY || it.state == DownloadWorkKeys.STATE_LEGACY_COMPLETED }
            ?.localPath
            ?.let(::File)
            ?.takeIf { it.exists() }

        return when {
            info?.state == WorkInfo.State.RUNNING && entity?.state == DownloadWorkKeys.STATE_VERIFYING ->
                DownloadState.Verifying

            info?.state == WorkInfo.State.RUNNING -> DownloadState.Downloading(
                progress = info.progress.getFloat(DownloadWorkKeys.KEY_PROGRESS, entity?.progress ?: 0f),
                bytesLoaded = info.progress.getLong(DownloadWorkKeys.KEY_BYTES_LOADED, entity?.bytesLoaded ?: 0L)
            )

            info?.state == WorkInfo.State.ENQUEUED || info?.state == WorkInfo.State.BLOCKED ->
                DownloadState.Downloading(0f, 0L)

            readyFile != null -> DownloadState.ReadyToInstall(readyFile.absolutePath)

            info?.state == WorkInfo.State.FAILED -> DownloadState.Failed(
                AppError.Unknown(info.outputData.getString(DownloadWorkKeys.KEY_ERROR) ?: "Download failed")
            )

            else -> DownloadState.Idle
        }
    }

    private fun DownloadEntity.toDownloadedApk(): DownloadedApk? {
        val file = localPath?.let(::File) ?: return null
        if (!file.exists()) return null
        return DownloadedApk(
            buildId = buildId,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            versionName = versionName,
            versionCode = versionCode,
            channel = channel,
            completedAt = completedAt ?: startedAt
        )
    }
}
```

- [ ] **Step 10: Запустить тесты репозитория**

Run: `./gradlew :core:downloads:testDebugUnitTest`
Expected: PASS

- [ ] **Step 11: Подключить модуль к feature:build-detail и перевести импорты DownloadState**

В `feature/build-detail/build.gradle.kts` в блок `dependencies` после `implementation(project(":core:database"))` добавить:

```kotlin
    implementation(project(":core:downloads"))
```

В `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/DownloadBuildUseCase.kt` удалить объявление `sealed interface DownloadState` (строки 12–18) и добавить импорт:

```kotlin
import com.appdist.core.downloads.DownloadState
```

В `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailScreen.kt:23` заменить импорт:

```kotlin
import com.appdist.core.downloads.DownloadState
```

В `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt` строка 15 (`import com.appdist.feature.builddetail.domain.*`) остаётся, дополнительно добавить:

```kotlin
import com.appdist.core.downloads.DownloadState
```

- [ ] **Step 12: Вызвать сверку зависших загрузок при старте приложения**

В `app/build.gradle.kts` в блок `dependencies` добавить (если ещё нет):

```kotlin
    implementation(project(":core:downloads"))
```

Заменить содержимое `app/src/main/kotlin/com/appdist/app/AppApplication.kt`:

```kotlin
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
    @Inject lateinit var downloadRepository: DownloadRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        appScope.launch { downloadRepository.reconcileStaleDownloads() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

- [ ] **Step 13: Проверить сборку всего проекта**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 14: Commit**

```bash
git add settings.gradle.kts core/downloads app feature/build-detail
git commit -m "feat(downloads): модуль core:downloads с наблюдением загрузки по имени уникальной работы"
```

---

### Task 4: DownloadWorker — проверка контрольной суммы внутри воркера

**Files:**
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/data/DownloadWorker.kt`
- Test: `feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/VerifyChecksumUseCaseTest.kt` (существует, не трогать)

**Interfaces:**
- Consumes: `DownloadWorkKeys`, `DownloadPaths` из Task 3; `VerifyChecksumUseCase` из `feature/build-detail/domain`
- Produces: воркер пишет в `downloads` состояния `downloading` → `verifying` → `ready`, а при ошибке — `failed`. При успехе `outputData` содержит `output_path`; при провале — `error`.

**ВНИМАНИЕ:** класс остаётся в пакете `com.appdist.feature.builddetail.data`. Не переносить.

- [ ] **Step 1: Переписать DownloadWorker**

Заменить содержимое `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/data/DownloadWorker.kt`:

```kotlin
package com.appdist.feature.builddetail.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.downloads.DownloadPaths
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.feature.builddetail.domain.VerifyChecksumUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * ВНИМАНИЕ: полное имя класса хранится в базе WorkManager. Перенос в другой
 * пакет или модуль сделает невозможным исполнение работ, поставленных в очередь
 * предыдущими версиями приложения.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    private val verifyChecksum: VerifyChecksumUseCase
) : CoroutineWorker(context, params) {

    companion object {
        /** Не чаще одной записи прогресса в БД за этот интервал. */
        private const val PROGRESS_WRITE_INTERVAL_MS = 250L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val buildId = inputData.getString(DownloadWorkKeys.KEY_BUILD_ID)
            ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(DownloadWorkKeys.KEY_DOWNLOAD_URL)
            ?: return@withContext Result.failure()
        val totalBytes = inputData.getLong(DownloadWorkKeys.KEY_FILE_SIZE, 0L)
        val expectedChecksum = inputData.getString(DownloadWorkKeys.KEY_EXPECTED_CHECKSUM)

        // Значения по умолчанию — для работ, поставленных в очередь до обновления:
        // у них этих ключей в inputData нет.
        val versionName = inputData.getString(DownloadWorkKeys.KEY_VERSION_NAME) ?: ""
        val versionCode = inputData.getLong(DownloadWorkKeys.KEY_VERSION_CODE, 0L)
        val channel = inputData.getString(DownloadWorkKeys.KEY_CHANNEL) ?: ""

        val outputFile = DownloadPaths.apkFile(applicationContext, buildId)
            .also { it.parentFile?.mkdirs() }

        fun row(state: String, progress: Float, loaded: Long, path: String?, completedAt: Long?) =
            DownloadEntity(
                buildId = buildId,
                localPath = path,
                state = state,
                progress = progress,
                bytesLoaded = loaded,
                totalBytes = totalBytes,
                startedAt = System.currentTimeMillis(),
                versionName = versionName,
                versionCode = versionCode,
                channel = channel,
                completedAt = completedAt
            )

        try {
            downloadDao.upsert(row(DownloadWorkKeys.STATE_DOWNLOADING, 0f, 0L, null, null))

            val request = Request.Builder().url(downloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
                    return@withContext if (response.code in 400..499) {
                        Result.failure(workDataOf(DownloadWorkKeys.KEY_ERROR to "HTTP ${response.code}"))
                    } else {
                        Result.retry()
                    }
                }
                val body = response.body ?: return@withContext Result.retry()

                outputFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastWriteAt = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                            setProgress(
                                workDataOf(
                                    DownloadWorkKeys.KEY_PROGRESS to progress,
                                    DownloadWorkKeys.KEY_BYTES_LOADED to totalRead
                                )
                            )
                            // Запись в БД троттлится: без этого на 14 МБ приходится
                            // около 1700 upsert-ов.
                            val now = System.currentTimeMillis()
                            if (now - lastWriteAt >= PROGRESS_WRITE_INTERVAL_MS) {
                                lastWriteAt = now
                                downloadDao.upsert(
                                    row(DownloadWorkKeys.STATE_DOWNLOADING, progress, totalRead, null, null)
                                )
                            }
                        }
                    }
                }
            }

            if (expectedChecksum != null) {
                downloadDao.upsert(row(DownloadWorkKeys.STATE_VERIFYING, 1f, totalBytes, null, null))
                val verified = verifyChecksum(outputFile, expectedChecksum)
                if (verified.isError) {
                    outputFile.delete()
                    downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
                    // Повтор не поможет: файл на сервере не совпадает с заявленной суммой.
                    return@withContext Result.failure(
                        workDataOf(DownloadWorkKeys.KEY_ERROR to "Контрольная сумма не совпала")
                    )
                }
            }

            downloadDao.upsert(
                row(
                    DownloadWorkKeys.STATE_READY,
                    1f,
                    totalBytes,
                    outputFile.absolutePath,
                    System.currentTimeMillis()
                )
            )
            Result.success(workDataOf(DownloadWorkKeys.KEY_OUTPUT_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            downloadDao.upsert(row(DownloadWorkKeys.STATE_FAILED, 0f, 0L, null, null))
            Result.retry()
        }
    }
}
```

- [ ] **Step 2: Проверить, что модуль компилируется**

Run: `./gradlew :feature:build-detail:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Убедиться, что существующие тесты модуля не сломаны**

Run: `./gradlew :feature:build-detail:testDebugUnitTest`
Expected: PASS для `VerifyChecksumUseCaseTest` и `GetInstallStatusUseCaseTest`. `BuildDetailViewModelTest` на этом шаге ещё может падать на компиляции — он будет переписан в Task 5. Если падает именно он, переходите к Task 5.

- [ ] **Step 4: Commit**

```bash
git add feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/data/DownloadWorker.kt
git commit -m "feat(build-detail): перенести проверку контрольной суммы в воркер и троттлить запись прогресса"
```

---

### Task 5: BuildDetailViewModel — подписка в init и починка «мёртвой» кнопки

Это задача, закрывающая баг из обратной связи.

**Files:**
- Create: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/StartDownloadUseCase.kt`
- Delete: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/DownloadBuildUseCase.kt`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt`
- Test: `feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/BuildDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `DownloadRepository.observeDownload`, `DownloadRepository.cancel`, `DownloadState` из Task 3
- Produces:
  - `StartDownloadUseCase.invoke(build: BuildUi, downloadUrl: String)` — ставит уникальную работу в очередь, ничего не возвращает
  - `BuildDetailViewModel(savedStateHandle, context, getBuildDetail, getInstallStatus, startDownload, downloadRepository, reportInstall)`

- [ ] **Step 1: Написать падающие тесты**

Заменить содержимое `feature/build-detail/src/test/kotlin/com/appdist/feature/builddetail/BuildDetailViewModelTest.kt`:

```kotlin
package com.appdist.feature.builddetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.BackendException
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadState
import com.appdist.feature.builddetail.domain.*
import com.appdist.feature.builddetail.ui.BuildDetailAction
import com.appdist.feature.builddetail.ui.BuildDetailEffect
import com.appdist.feature.builddetail.ui.BuildDetailViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getBuildDetail: GetBuildDetailUseCase
    private lateinit var getInstallStatus: GetInstallStatusUseCase
    private lateinit var startDownload: StartDownloadUseCase
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var reportInstall: ReportInstallUseCase
    private lateinit var context: android.content.Context

    private val fakeBuild = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0", versionCode = 1L, channel = "alpha", environment = "dev",
        buildType = "debug", changelog = null, fileSize = 1024L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", minSdk = 24, targetSdk = 35,
        certFingerprint = null, abis = emptyList(), expiryDate = null,
        branch = null, commitHash = null
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getBuildDetail = mockk()
        getInstallStatus = mockk()
        startDownload = mockk(relaxed = true)
        downloadRepository = mockk(relaxed = true)
        reportInstall = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.packageName } returns "com.appdist.app"

        every { getBuildDetail(any<String>()) } returns flowOf(fakeBuild)
        every { getInstallStatus(any(), any(), any(), any()) } returns InstallStatus.NotInstalled
        every { downloadRepository.observeDownload("b1") } returns flowOf(DownloadState.Idle)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel() = BuildDetailViewModel(
        SavedStateHandle(mapOf("buildId" to "b1")),
        context, getBuildDetail, getInstallStatus, startDownload, downloadRepository, reportInstall
    )

    @Test
    fun `loads build on init`() = runTest {
        createViewModel().state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("b1", state.build?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `copy link emits CopyToClipboard effect`() = runTest {
        val vm = createViewModel()
        vm.effects.test {
            vm.onAction(BuildDetailAction.CopyLinkClicked)
            val effect = awaitItem()
            assertTrue(effect is BuildDetailEffect.CopyToClipboard)
            assertEquals("appdist://builds/b1", (effect as BuildDetailEffect.CopyToClipboard).text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state set when build load fails`() = runTest {
        every { getBuildDetail(any<String>()) } returns flow { throw BackendException(503, "Unavailable") }
        createViewModel().state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(503, (state.error as AppError.Network).code)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Регрессия на баг: возврат на экран во время активной загрузки ---

    @Test
    fun `restores Downloading state on fresh view model when work is running`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.Downloading(0.35f, 350L))

        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(DownloadState.Downloading(0.35f, 350L), state.downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restores ReadyToInstall state without launching installer automatically`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.ReadyToInstall("/data/b1.apk"))

        val vm = createViewModel()
        vm.state.test {
            assertEquals(DownloadState.ReadyToInstall("/data/b1.apk"), awaitItem().downloadState)
            cancelAndIgnoreRemainingEvents()
        }
        // Восстановленное состояние не должно само открывать системный установщик
        coVerify(exactly = 0) { reportInstall(any()) }
    }

    @Test
    fun `does not enqueue a second download when file is already downloaded`() = runTest {
        every { downloadRepository.observeDownload("b1") } returns
            flowOf(DownloadState.ReadyToInstall("/data/b1.apk"))

        val vm = createViewModel()
        vm.onAction(BuildDetailAction.DownloadOrInstallClicked)

        verify(exactly = 0) { startDownload(any(), any()) }
    }

    @Test
    fun `button click enqueues download and state follows repository`() = runTest {
        val downloadFlow = MutableStateFlow<DownloadState>(DownloadState.Idle)
        every { downloadRepository.observeDownload("b1") } returns downloadFlow
        coEvery { getBuildDetail.getDownloadUrl("b1") } returns Result.Success("https://cdn/b1.apk")

        val vm = createViewModel()
        vm.state.test {
            assertEquals(DownloadState.Idle, awaitItem().downloadState)

            vm.onAction(BuildDetailAction.DownloadOrInstallClicked)
            verify { startDownload(fakeBuild, "https://cdn/b1.apk") }

            // Прогресс приходит из подписки, а не из результата постановки в очередь
            downloadFlow.value = DownloadState.Downloading(0.5f, 500L)
            assertEquals(DownloadState.Downloading(0.5f, 500L), awaitItem().downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel delegates to repository`() = runTest {
        val vm = createViewModel()
        vm.onAction(BuildDetailAction.CancelDownload)
        coVerify { downloadRepository.cancel("b1") }
    }
}
```

- [ ] **Step 2: Запустить тесты и убедиться, что они падают**

Run: `./gradlew :feature:build-detail:testDebugUnitTest --tests "*BuildDetailViewModelTest*"`
Expected: FAIL с ошибкой компиляции — `StartDownloadUseCase` не существует, конструктор ViewModel не совпадает

- [ ] **Step 3: Создать StartDownloadUseCase**

Создать `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/StartDownloadUseCase.kt`:

```kotlin
package com.appdist.feature.builddetail.domain

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.appdist.core.common.model.BuildUi
import com.appdist.core.downloads.DownloadWorkKeys
import com.appdist.feature.builddetail.data.DownloadWorker
import javax.inject.Inject

/**
 * Ставит загрузку в очередь и ничего не возвращает.
 *
 * Наблюдать за результатом по UUID этого запроса нельзя: при
 * ExistingWorkPolicy.KEEP уже идущая работа сохраняется, а новый запрос
 * отбрасывается и в базе WorkManager не появляется. Состояние читается
 * через DownloadRepository.observeDownload по имени уникальной работы.
 */
class StartDownloadUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    operator fun invoke(build: BuildUi, downloadUrl: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorkKeys.KEY_BUILD_ID to build.id,
                    DownloadWorkKeys.KEY_DOWNLOAD_URL to downloadUrl,
                    DownloadWorkKeys.KEY_EXPECTED_CHECKSUM to build.checksumSha256,
                    DownloadWorkKeys.KEY_FILE_SIZE to build.fileSize,
                    DownloadWorkKeys.KEY_VERSION_NAME to build.versionName,
                    DownloadWorkKeys.KEY_VERSION_CODE to build.versionCode,
                    DownloadWorkKeys.KEY_CHANNEL to build.channel
                )
            )
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork(
            "download_${build.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
```

- [ ] **Step 4: Удалить DownloadBuildUseCase**

```bash
git rm feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/domain/DownloadBuildUseCase.kt
```

- [ ] **Step 5: Переписать BuildDetailViewModel**

Заменить содержимое `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt`:

```kotlin
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
            downloadRepository.observeDownload(buildId).collect { downloadState ->
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
```

- [ ] **Step 6: Запустить тесты**

Run: `./gradlew :feature:build-detail:testDebugUnitTest`
Expected: PASS, все тесты включая новые регрессионные

- [ ] **Step 7: Проверить сборку проекта**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add feature/build-detail
git commit -m "fix(build-detail): восстанавливать состояние загрузки при возврате на экран

Состояние наблюдалось по UUID запроса, который при ExistingWorkPolicy.KEEP
отбрасывался, если работа уже шла. Flow вечно отдавал null, состояние
оставалось Idle, и кнопка не реагировала на нажатие. Теперь наблюдение
идёт по имени уникальной работы, а подписка живёт в init ViewModel."
```

---

### Task 6: Раздел «Скачанные» в «Мои сборки»

**Files:**
- Modify: `feature/mine/build.gradle.kts`
- Modify: `feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineViewModel.kt`
- Modify: `feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineScreen.kt`
- Test: `feature/mine/src/test/kotlin/com/appdist/feature/mine/MineViewModelTest.kt`

**Interfaces:**
- Consumes: `DownloadRepository.observeDownloaded`, `DownloadRepository.delete`, `DownloadRepository.deleteAll`, `DownloadedApk` из Task 3; `BuildDao.getBuildById` из `:core:database`
- Produces:
  - `DownloadedApkUi(buildId: String, title: String, channel: String, sizeBytes: Long, completedAt: Long)`
  - `MineUiState(installHistory, activeDownloads, downloaded: List<DownloadedApkUi>)` со свойством `downloadedTotalBytes: Long`
  - `MineAction.DeleteDownload(buildId: String)`, `MineAction.DeleteAllDownloads`

- [ ] **Step 1: Написать падающие тесты**

Заменить содержимое `feature/mine/src/test/kotlin/com/appdist/feature/mine/MineViewModelTest.kt`:

```kotlin
package com.appdist.feature.mine

import app.cash.turbine.test
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadedApk
import com.appdist.feature.mine.ui.MineAction
import com.appdist.feature.mine.ui.MineViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var installHistoryDao: InstallHistoryDao
    private lateinit var downloadDao: DownloadDao
    private lateinit var buildDao: BuildDao
    private lateinit var downloadRepository: DownloadRepository

    private val fakeHistory = listOf(
        InstallHistoryEntity("id1", "b1", "com.test", "1.0", 1L, System.currentTimeMillis())
    )
    private val fakeDownloads = listOf(
        DownloadEntity("b2", null, "downloading", 0.5f, 512L, 1024L, System.currentTimeMillis())
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        installHistoryDao = mockk()
        downloadDao = mockk()
        buildDao = mockk()
        downloadRepository = mockk(relaxed = true)
        every { installHistoryDao.getAll() } returns flowOf(fakeHistory)
        every { downloadDao.getActiveDownloads() } returns flowOf(fakeDownloads)
        every { downloadRepository.observeDownloaded() } returns flowOf(emptyList())
        coEvery { buildDao.getBuildById(any()) } returns null
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel() =
        MineViewModel(installHistoryDao, downloadDao, buildDao, downloadRepository)

    @Test
    fun `combines install history and active downloads`() = runTest {
        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(1, state.installHistory.size)
            assertEquals("com.test", state.installHistory[0].packageName)
            assertEquals(1, state.activeDownloads.size)
            assertEquals("b2", state.activeDownloads[0].buildId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `downloaded section shows version from row and sums total size`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(
                downloadedApk("d1", versionName = "2.1", versionCode = 21L, size = 1024L),
                downloadedApk("d2", versionName = "2.2", versionCode = 22L, size = 2048L)
            )
        )

        createViewModel().state.test {
            val state = awaitItem()
            assertEquals(2, state.downloaded.size)
            assertEquals("v2.1 (21)", state.downloaded[0].title)
            assertEquals(3072L, state.downloadedTotalBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row without version falls back to builds cache`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("legacy", versionName = "", versionCode = 0L, size = 10L))
        )
        coEvery { buildDao.getBuildById("legacy") } returns buildEntity("legacy", "3.0", 30L)

        createViewModel().state.test {
            assertEquals("v3.0 (30)", awaitItem().downloaded[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy row with nothing known falls back to short build id`() = runTest {
        every { downloadRepository.observeDownloaded() } returns flowOf(
            listOf(downloadedApk("0123456789abcdef", versionName = "", versionCode = 0L, size = 10L))
        )
        coEvery { buildDao.getBuildById("0123456789abcdef") } returns null

        createViewModel().state.test {
            assertEquals("Сборка 01234567", awaitItem().downloaded[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete download delegates to repository and list shrinks`() = runTest {
        val downloaded = MutableStateFlow(
            listOf(
                downloadedApk("d1", versionName = "2.1", versionCode = 21L, size = 1024L),
                downloadedApk("d2", versionName = "2.2", versionCode = 22L, size = 2048L)
            )
        )
        every { downloadRepository.observeDownloaded() } returns downloaded

        val vm = createViewModel()
        vm.state.test {
            assertEquals(2, awaitItem().downloaded.size)

            vm.onAction(MineAction.DeleteDownload("d1"))
            coVerify { downloadRepository.delete("d1") }

            downloaded.value = downloaded.value.filterNot { it.buildId == "d1" }
            val after = awaitItem()
            assertEquals(1, after.downloaded.size)
            assertEquals(2048L, after.downloadedTotalBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete all downloads delegates to repository`() = runTest {
        val vm = createViewModel()
        vm.onAction(MineAction.DeleteAllDownloads)
        coVerify { downloadRepository.deleteAll() }
    }

    private fun downloadedApk(buildId: String, versionName: String, versionCode: Long, size: Long) =
        DownloadedApk(
            buildId = buildId,
            filePath = "/data/$buildId.apk",
            sizeBytes = size,
            versionName = versionName,
            versionCode = versionCode,
            channel = "alpha",
            completedAt = 1000L
        )

    private fun buildEntity(id: String, versionName: String, versionCode: Long) = BuildEntity(
        id = id, projectId = "p1", versionName = versionName, versionCode = versionCode,
        channel = "alpha", environment = "qa", changelog = null, fileSize = 10L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", cachedAt = 0L
    )
}
```

- [ ] **Step 2: Запустить тесты и убедиться, что они падают**

Run: `./gradlew :feature:mine:testDebugUnitTest`
Expected: FAIL с ошибкой компиляции — `MineAction` не существует, конструктор `MineViewModel` не совпадает

- [ ] **Step 3: Подключить :core:downloads к feature:mine**

В `feature/mine/build.gradle.kts` в блок `dependencies` после `implementation(project(":core:database"))` добавить:

```kotlin
    implementation(project(":core:downloads"))
```

- [ ] **Step 4: Переписать MineViewModel**

Заменить содержимое `feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineViewModel.kt`:

```kotlin
package com.appdist.feature.mine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import com.appdist.core.downloads.DownloadRepository
import com.appdist.core.downloads.DownloadedApk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadedApkUi(
    val buildId: String,
    val title: String,
    val channel: String,
    val sizeBytes: Long,
    val completedAt: Long
)

data class MineUiState(
    val installHistory: List<InstallHistoryEntity> = emptyList(),
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val downloaded: List<DownloadedApkUi> = emptyList()
) {
    val downloadedTotalBytes: Long get() = downloaded.sumOf { it.sizeBytes }
}

sealed interface MineAction {
    data class DeleteDownload(val buildId: String) : MineAction
    data object DeleteAllDownloads : MineAction
}

@HiltViewModel
class MineViewModel @Inject constructor(
    installHistoryDao: InstallHistoryDao,
    downloadDao: DownloadDao,
    private val buildDao: BuildDao,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val state: StateFlow<MineUiState> = combine(
        installHistoryDao.getAll(),
        downloadDao.getActiveDownloads(),
        downloadRepository.observeDownloaded()
    ) { history, downloads, downloaded ->
        MineUiState(
            installHistory = history,
            activeDownloads = downloads,
            downloaded = downloaded.map { it.toUi() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())

    fun onAction(action: MineAction) {
        when (action) {
            is MineAction.DeleteDownload -> viewModelScope.launch {
                downloadRepository.delete(action.buildId)
            }
            MineAction.DeleteAllDownloads -> viewModelScope.launch {
                downloadRepository.deleteAll()
            }
        }
    }

    /**
     * У строк, созданных версиями до миграции 1 → 2, метаданных сборки нет:
     * versionName пустой, versionCode нулевой. Подставляем по цепочке
     * строка → кэш builds → короткий идентификатор.
     */
    private suspend fun DownloadedApk.toUi(): DownloadedApkUi {
        val title = when {
            versionName.isNotBlank() -> "v$versionName ($versionCode)"
            else -> {
                val cached = buildDao.getBuildById(buildId)
                if (cached != null) "v${cached.versionName} (${cached.versionCode})"
                else "Сборка ${buildId.take(8)}"
            }
        }
        return DownloadedApkUi(
            buildId = buildId,
            title = title,
            channel = channel,
            sizeBytes = sizeBytes,
            completedAt = completedAt
        )
    }
}
```

`combine` с suspend-преобразованием внутри работает: лямбда `combine` — suspend-функция.

- [ ] **Step 5: Запустить тесты ViewModel**

Run: `./gradlew :feature:mine:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: Добавить раздел «Скачанные» в MineScreen**

Заменить содержимое `feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineScreen.kt`:

```kotlin
package com.appdist.feature.mine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    onBuildClick: (String) -> Unit,
    viewModel: MineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Удалить все скачанные файлы?") },
            text = { Text("Освободится ${formatSize(state.downloadedTotalBytes)}. Сборки можно будет скачать заново.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(MineAction.DeleteAllDownloads)
                    showClearAllDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Мои сборки") }) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Active downloads section
            if (state.activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        "Загрузки",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(state.activeDownloads, key = { it.buildId }) { download ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(download.buildId, style = MaterialTheme.typography.bodyMedium)
                            val stateLabel = when (download.state) {
                                "downloading" -> "Загрузка…"
                                "verifying" -> "Проверка…"
                                "failed" -> "Ошибка"
                                else -> download.state
                            }
                            Text(
                                stateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { download.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (download.totalBytes > 0) {
                                Text(
                                    "${download.bytesLoaded / 1024 / 1024} / ${download.totalBytes / 1024 / 1024} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Downloaded files section
            if (state.downloaded.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Скачанные · ${formatSize(state.downloadedTotalBytes)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text("Очистить всё")
                        }
                    }
                }
                items(state.downloaded, key = { it.buildId }) { apk ->
                    val dateStr = remember(apk.completedAt) { dateFormatter.format(Date(apk.completedAt)) }
                    ElevatedCard(
                        onClick = { onBuildClick(apk.buildId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
                        ) {
                            Column(
                                Modifier.weight(1f).padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(apk.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${apk.channel} · ${formatSize(apk.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.onAction(MineAction.DeleteDownload(apk.buildId)) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить файл")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Install history section
            item {
                Text(
                    "История установок",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (state.installHistory.isEmpty()) {
                item {
                    Text(
                        "Нет установленных сборок",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.installHistory, key = { it.id }) { entry ->
                    val dateStr = remember(entry.installedAt) {
                        dateFormatter.format(Date(entry.installedAt))
                    }
                    ElevatedCard(
                        onClick = { onBuildClick(entry.buildId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.packageName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "v${entry.versionName} (${entry.versionCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "${bytes / 1024} КБ"
    else -> "$bytes Б"
}
```

- [ ] **Step 7: Добавить зависимость на иконки в feature:mine**

`Icons.Default.DeleteOutline` живёт в material-icons-extended. В `feature/mine/build.gradle.kts` в блок `dependencies` добавить:

```kotlin
    implementation(libs.compose.material.icons)
```

- [ ] **Step 8: Запустить тесты и сборку**

Run: `./gradlew :feature:mine:testDebugUnitTest && ./gradlew assembleDebug`
Expected: PASS и BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add feature/mine
git commit -m "feat(mine): раздел «Скачанные» с удалением файлов по одному и целиком"
```

---

### Task 7: Финальная проверка

**Files:** нет изменений, только проверки

- [ ] **Step 1: Прогнать все тесты проекта**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, все модули зелёные

- [ ] **Step 2: Собрать debug-сборку**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Проверить инварианты обратной совместимости grep-ом**

```bash
grep -rn "download_\$buildId\|download_\${build.id}" --include=*.kt . | grep -v "/build/"
grep -rn "apk_downloads" --include=*.kt --include=*.xml . | grep -v "/build/"
grep -rn "package com.appdist.feature.builddetail.data" feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/data/DownloadWorker.kt
grep -n "fileprovider" feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailViewModel.kt
```

Expected:
- имя уникальной работы встречается в `DownloadRepository.uniqueWorkName` и `StartDownloadUseCase`, обе формы дают `download_<buildId>`
- `apk_downloads` встречается в `DownloadPaths.kt` и `app/src/main/res/xml/file_paths.xml`
- пакет воркера — `com.appdist.feature.builddetail.data`
- authority — `${context.packageName}.fileprovider`

- [ ] **Step 4: Ручная проверка на устройстве**

Сценарий из обратной связи:

1. Открыть экран сборки, нажать «Скачать и установить».
2. Дождаться появления индикатора, нажать «Назад».
3. Вернуться на экран сборки — **индикатор должен быть на месте и показывать актуальный прогресс**, а не кнопку.
4. Дождаться окончания загрузки — открывается системный установщик.
5. Отменить установку, выйти с экрана и зайти снова — установщик **не должен** открываться сам, кнопка должна показывать «Установить».
6. Открыть «Мои сборки» — в разделе «Скачанные» есть файл с версией, каналом и размером.
7. Удалить файл корзиной — исчезает из списка, суммарный размер уменьшается.
8. Скачать ещё две сборки, нажать «Очистить всё» — после подтверждения раздел пропадает.

- [ ] **Step 5: Проверка обновления поверх старой версии**

Требование «не ломать уже собранное приложение» проверяется только так:

1. Установить APK предыдущей версии (до этих изменений).
2. Скачать в ней хотя бы одну сборку до конца и оставить историю установок непустой.
3. Установить поверх новую сборку (`./gradlew assembleDebug` + `adb install -r`).
4. Открыть «Мои сборки»: **история установок сохранилась**, скачанный ранее файл виден в разделе «Скачанные» (заголовок может быть вида «Сборка 01234567» — это ожидаемо для строк без метаданных).
5. Открыть экран той сборки — кнопка показывает «Установить», повторная загрузка не запускается.
