package com.shamsalmaarif.reader.data.repository

import com.shamsalmaarif.reader.data.database.dao.ReadsDao
import com.shamsalmaarif.reader.data.database.entities.ReadEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadsRepository @Inject constructor(private val dao: ReadsDao) {

    fun getAllReads(): Flow<List<ReadEntity>> = dao.getAllReads()

    fun getArchivedReads(): Flow<List<ReadEntity>> = dao.getArchivedReads()

    fun searchReads(query: String): Flow<List<ReadEntity>> = dao.searchReads(query)

    suspend fun getReadById(id: String): ReadEntity? = dao.getReadById(id)

    suspend fun insertRead(read: ReadEntity) = dao.insertRead(read)

    suspend fun updateRead(read: ReadEntity) = dao.updateRead(read)

    suspend fun updateProgress(id: String, offset: Int) = dao.updateProgress(id, offset)

    suspend fun updateLanguage(id: String, lang: String) = dao.updateLanguage(id, lang)

    suspend fun updateSpeed(id: String, speed: Float) = dao.updateSpeed(id, speed)

    suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived)

    suspend fun deleteRead(id: String) = dao.deleteRead(id)
}
