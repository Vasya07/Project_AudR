package com.example.project_audr.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_records",
    indices = [
        Index(value = ["title"]),
        Index(value = ["dateCreated"])
    ]
)
data class AudioRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val filePath: String,

    val duration: Int,

    val dateCreated: Long,

    val fileSize: Long,

    val isImported: Boolean = false,

    val isFavorite: Boolean = false,

    val bitrate: Int? = null,

    val sampleRate: Int? = null
)