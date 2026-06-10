package com.shamsalmaarif.reader.data.database.dao

import androidx.room.*
import com.shamsalmaarif.reader.data.database.entities.ReadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadsDao {

    @Query("SELECT * FROM reads WHERE isArchived = 0 ORDER BY addedAtUnix DESC")
    fun getAllReads(): Flow<List<ReadEntity>>

    @Query("SELECT * FROM reads WHERE isArchived = 1 ORDER BY updatedAtUnix DESC")
    fun getArchivedReads(): Flow<List<ReadEntity>>

    @Query("SELECT * FROM reads WHERE readId = :id")
    suspend fun getReadById(id: String): ReadEntity?

    @Query("SELECT * FROM reads WHERE (title LIKE '%' || :q || '%' OR author LIKE '%' || :q || '%' OR url LIKE '%' || :q || '%') AND isArchived = 0")
    fun searchReads(q: String): Flow<List<ReadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRead(read: ReadEntity)

    @Update
    suspend fun updateRead(read: ReadEntity)

    @Query("UPDATE reads SET lastListenedCharOffset = :offset, updatedAtUnix = :ts WHERE readId = :id")
    suspend fun updateProgress(id: String, offset: Int, ts: Long = System.currentTimeMillis() / 1000)

    @Query("UPDATE reads SET lastUsedLanguage = :lang WHERE readId = :id")
    suspend fun updateLanguage(id: String, lang: String)

    @Query("UPDATE reads SET lastUsedSpeed = :speed WHERE readId = :id")
    suspend fun updateSpeed(id: String, speed: Float)

    @Query("UPDATE reads SET isArchived = :archived WHERE readId = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("DELETE FROM reads WHERE readId = :id")
    suspend fun deleteRead(id: String)

    @Query("SELECT COUNT(*) FROM reads WHERE isArchived = 0")
    suspend fun getCount(): Int
}
