package com.example.comparechart

import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val scoreDao: ScoreDao) {
    val allScores: Flow<List<Score>> = scoreDao.getAllScores()

    suspend fun insertScore(score: Score) {
        scoreDao.insertScore(score)
    }
    suspend fun deleteScore(score: Score) {
        scoreDao.deleteScore(score)
    }
    suspend fun deleteAllScores() {
        scoreDao.deleteAllScores()
    }
}