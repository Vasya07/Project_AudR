package com.example.project_audr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRecordDao {

    @Query("SELECT * FROM audio_records ORDER BY dateCreated DESC")
    fun getAllRecords(): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE id = :id")
    suspend fun getRecordById(id: Int): AudioRecordEntity?

    @Query("SELECT * FROM audio_records WHERE title LIKE '%' || :query || '%' ORDER BY dateCreated DESC")
    fun searchRecords(query: String): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE isFavorite = 1 ORDER BY dateCreated DESC")
    fun getFavoriteRecords(): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE isImported = :isImported ORDER BY dateCreated DESC")
    fun getRecordsByType(isImported: Boolean): Flow<List<AudioRecordEntity>>

    @Insert
    suspend fun insert(record: AudioRecordEntity): Long

    @Update
    suspend fun update(record: AudioRecordEntity)

    @Delete
    suspend fun delete(record: AudioRecordEntity)

    @Query("DELETE FROM audio_records WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM audio_records")
    suspend fun deleteAll()

    @Query("UPDATE audio_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM audio_records")
    suspend fun getRecordCount(): Int

    @Query("SELECT SUM(fileSize) FROM audio_records")
    suspend fun getTotalSize(): Long?
}