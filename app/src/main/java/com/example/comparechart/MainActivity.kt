package com.example.comparechart

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
    @Delete
    suspend fun deleteScore(score: Score)
    @Query("DELETE FROM Score")
    suspend fun deleteAllScores()
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
    suspend fun deleteScore(score: Score) {
        scoreDao.deleteScore(score)
    }

    suspend fun deleteAllScores() {
        scoreDao.deleteAllScores()
    }
}

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

    // ダイアログの表示状態を管理するフラグ
    var showLastScoreDialog by remember { mutableStateOf(false) }
    var showAllScoresDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        var date by remember { mutableStateOf(TextFieldValue()) }
        var reading by remember { mutableStateOf(TextFieldValue()) }
        var listening by remember { mutableStateOf(TextFieldValue()) }
        var writing by remember { mutableStateOf(TextFieldValue()) }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = date.text,
            onValueChange = { date = TextFieldValue(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("日付") }
        )
        OutlinedTextField(
            value = reading.text,
            onValueChange = { reading = TextFieldValue(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reading") }
        )
        OutlinedTextField(
            value = listening.text,
            onValueChange = { listening = TextFieldValue(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Listening") }
        )
        OutlinedTextField(
            value = writing.text,
            onValueChange = { writing = TextFieldValue(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Writing") }
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        // 最後のスコア削除ボタン
        Button(
            onClick = {
                if (scores.isNotEmpty()) {
                    showLastScoreDialog = true
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("最後のスコアを削除")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 全てのスコア削除ボタン
        Button(
            onClick = {
                showAllScoresDialog = true
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("全てのスコアを削除")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScoreChart(scores)

        // 最後のスコア削除確認ダイアログ
        if (showLastScoreDialog) {
            ConfirmDeleteDialog(
                message = "最後のスコアを削除しますか？",
                onConfirm = {
                    viewModel.deleteScore(scores.last())
                    showLastScoreDialog = false
                },
                onDismiss = { showLastScoreDialog = false }
            )
        }

        // 全てのスコア削除確認ダイアログ
        if (showAllScoresDialog) {
            ConfirmDeleteDialog(
                message = "全てのスコアを削除しますか？",
                onConfirm = {
                    viewModel.deleteAllScores()
                    showAllScoresDialog = false
                },
                onDismiss = { showAllScoresDialog = false }
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("削除")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("キャンセル")
            }
        },
        text = { Text(message) }
    )
}



@Preview(showBackground = true)
@Composable
private fun MainContentPreview(
    @PreviewParameter(PreviewParameterProvider::class)
    viewModel: ScoreViewModel
) {
    CompareChartTheme() {
        MainContent(viewModel = viewModel)
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

        val dataSetReading = LineDataSet(entriesReading, "リーディングスコア").apply { color = Color.RED }
        val dataSetListening = LineDataSet(entriesListening, "リスニングスコア").apply { color = Color.BLUE }
        val dataSetWriting = LineDataSet(entriesWriting, "ライティングスコア").apply { color = Color.YELLOW }

        chart.data = LineData(dataSetReading, dataSetListening, dataSetWriting)
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dates)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
        }
        chart.invalidate()
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreChartPreview() {
    CompareChartTheme() {
        ScoreChart(
            scores = TODO()
        )
    }
}