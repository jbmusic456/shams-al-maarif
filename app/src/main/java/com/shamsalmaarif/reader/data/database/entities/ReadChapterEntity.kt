package com.shamsalmaarif.reader.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "read_chapters",
    foreignKeys = [ForeignKey(
        entity = ReadEntity::class,
        parentColumns = ["readId"],
        childColumns = ["readId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("readId")]
)
data class ReadChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val readId: String,
    val chapterIndex: Int,
    val title: String?,
    val charStart: Int,
    val charEnd: Int
)
