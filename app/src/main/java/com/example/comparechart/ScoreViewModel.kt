package com.example.comparechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScoreViewModel(private val repository: ScoreRepository) : ViewModel() {
    val scores = repository.allScores

    fun addScore(date: String, reading: Float, listening: Float, writing: Float) {
        viewModelScope.launch {
            repository.insertScore(Score(date = date, reading = reading, listening = listening, writing = writing))
        }
    }
    fun deleteScore(score: Score) {
        viewModelScope.launch {
            repository.deleteScore(score)
        }
    }
    fun deleteAllScores() {
        viewModelScope.launch {
            repository.deleteAllScores()
        }
    }
}