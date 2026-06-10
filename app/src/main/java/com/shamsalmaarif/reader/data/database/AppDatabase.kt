package com.shamsalmaarif.reader.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shamsalmaarif.reader.data.database.dao.ReadsDao
import com.shamsalmaarif.reader.data.database.entities.ReadChapterEntity
import com.shamsalmaarif.reader.data.database.entities.ReadEntity

@Database(
    entities = [ReadEntity::class, ReadChapterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readsDao(): ReadsDao
}
