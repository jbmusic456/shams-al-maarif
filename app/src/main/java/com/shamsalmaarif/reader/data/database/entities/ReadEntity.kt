package com.shamsalmaarif.reader.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reads")
data class ReadEntity(
    @PrimaryKey val readId: String,
    val title: String?,
    val author: String?,
    val description: String?,
    val url: String?,
    val source: String?,
    val originalFileType: String?,
    val contentType: String?,
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val lastListenedCharOffset: Int = 0,
    val lastUsedLanguage: String = "en",
    val lastUsedSpeed: Float = 1.0f,
    val createdAtUnix: Long = System.currentTimeMillis() / 1000,
    val updatedAtUnix: Long = System.currentTimeMillis() / 1000,
    val addedAtUnix: Long = System.currentTimeMillis() / 1000,
    val isArchived: Boolean = false,
    val fromUserImport: Boolean = true,
    val articleImageUrl: String? = null,
    val genre: String = "",
    val matureContent: Boolean = false
)
