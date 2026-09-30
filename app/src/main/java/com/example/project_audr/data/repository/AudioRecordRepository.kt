package com.example.project_audr.data.repository

import com.example.project_audr.data.local.AudioRecordDao
import com.example.project_audr.data.local.AudioRecordEntity
import kotlinx.coroutines.flow.Flow

class AudioRecordRepository(
    private val dao: AudioRecordDao
) {

    fun getAllRecords(): Flow<List<AudioRecordEntity>> = dao.getAllRecords()

    fun searchRecords(query: String): Flow<List<AudioRecordEntity>> = dao.searchRecords(query)

    fun getFavorites(): Flow<List<AudioRecordEntity>> = dao.getFavoriteRecords()

    fun getByType(imported: Boolean): Flow<List<AudioRecordEntity>> = dao.getRecordsByType(imported)

    suspend fun getRecordById(id: Int): AudioRecordEntity? = dao.getRecordById(id)

    suspend fun insertRecord(record: AudioRecordEntity): Long = dao.insert(record)

    suspend fun updateRecord(record: AudioRecordEntity) = dao.update(record)

    suspend fun deleteRecord(record: AudioRecordEntity) = dao.delete(record)

    suspend fun deleteRecordById(id: Int) = dao.deleteById(id)

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) =
        dao.updateFavoriteStatus(id, isFavorite)

    suspend fun getRecordCount(): Int = dao.getRecordCount()

    suspend fun getTotalSize(): Long? = dao.getTotalSize()

    suspend fun deleteAll() = dao.deleteAll()
}