package com.example.comparechart

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: Score)
    @Delete
    suspend fun deleteScore(score: Score)
    @Query("DELETE FROM Score")
    suspend fun deleteAllScores()
    @Query("SELECT * FROM Score")
    fun getAllScores(): Flow<List<Score>>
}