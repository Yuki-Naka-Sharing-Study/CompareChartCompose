package com.example.comparechart

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.example.comparechart.ui.theme.CompareChartTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val reading: Float,
    val listening: Float,
    val writing: Float
)

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM Score")
    fun getAllScores(): Flow<List<Score>>
}

@Database(entities = [Score::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}

class ScoreRepository(private val scoreDao: ScoreDao) {
    val allScores: Flow<List<Score>> = scoreDao.getAllScores()

    suspend fun insertScore(score: Score) {
        scoreDao.insertScore(score)
    }
}

class ScoreViewModel(private val repository: ScoreRepository) : ViewModel() {
    val scores = repository.allScores

    fun addScore(date: String, reading: Float, listening: Float, writing: Float) {
        viewModelScope.launch {
            repository.insertScore(Score(date = date, reading = reading, listening = listening, writing = writing))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "score_db").build()
        val repository = ScoreRepository(database.scoreDao())
        val viewModel = ScoreViewModel(repository)

        setContent {
            CompareChartTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: ScoreViewModel) {
    val scores by viewModel.scores.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        var date by remember { mutableStateOf(TextFieldValue()) }
        var reading by remember { mutableStateOf(TextFieldValue()) }
        var listening by remember { mutableStateOf(TextFieldValue()) }
        var writing by remember { mutableStateOf(TextFieldValue()) }

        BasicTextField(
            value = date,
            onValueChange = { date = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField -> Box { innerTextField() } }
        )

        BasicTextField(
            value = reading,
            onValueChange = { reading = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField -> Box { innerTextField() } }
        )

        BasicTextField(
            value = listening,
            onValueChange = { listening = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField -> Box { innerTextField() } }
        )

        BasicTextField(
            value = writing,
            onValueChange = { writing = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField -> Box { innerTextField() } }
        )

        Button(onClick = {
            viewModel.addScore(
                date.text,
                reading.text.toFloat(),
                listening.text.toFloat(),
                writing.text.toFloat()
            )
        }) {
            Text("スコアを追加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScoreChart(scores)
    }
}

@Composable
fun ScoreChart(scores: List<Score>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) { chart ->
        val entriesReading = scores.mapIndexed { index, score -> Entry(index.toFloat(), score.reading) }
        val entriesListening = scores.mapIndexed { index, score -> Entry(index.toFloat(), score.listening) }
        val entriesWriting = scores.mapIndexed { index, score -> Entry(index.toFloat(), score.writing) }
        val dates = scores.map { it.date }

        val dataSetReading = LineDataSet(entriesReading, "リーディングスコア").apply { color = android.graphics.Color.RED }
        val dataSetListening = LineDataSet(entriesListening, "リスニングスコア").apply { color = android.graphics.Color.BLUE }
        val dataSetWriting = LineDataSet(entriesWriting, "ライティングスコア").apply { color = android.graphics.Color.YELLOW }

        chart.data = LineData(dataSetReading, dataSetListening, dataSetWriting)
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dates)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
        }
        chart.invalidate()
    }
}
