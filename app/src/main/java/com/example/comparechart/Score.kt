package com.example.comparechart

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val reading: Float,
    val listening: Float,
    val writing: Float
)