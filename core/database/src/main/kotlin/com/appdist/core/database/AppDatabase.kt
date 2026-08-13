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
