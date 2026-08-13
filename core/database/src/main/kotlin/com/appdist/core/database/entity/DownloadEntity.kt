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
