package com.example.comparechart

import android.app.DatePickerDialog
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.*
import com.example.comparechart.ui.theme.CompareChartTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "score_db").build()
        val repository = ScoreRepository(database.scoreDao())
        val viewModel = ScoreViewModel(repository)

        setContent {
            val context = this
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

    var showLatestScoreDeleteDialog by remember { mutableStateOf(false) }
    var showAllScoresDeleteDialog by remember { mutableStateOf(false) }
    var showDuplicateDateErrorDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var maxDataErrorDialog by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }

    var reading by remember { mutableStateOf(TextFieldValue()) }
    var listening by remember { mutableStateOf(TextFieldValue()) }
    var writing by remember { mutableStateOf(TextFieldValue()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        SelectDatePicker(LocalContext.current) { date->
            selectedDate = date
        }
        Text("受験日: $selectedDate")

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

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = {
            if (scores.any { it.date == selectedDate }) {
                showDuplicateDateErrorDialog = true
            } else {
                val readingScore = reading.text.toFloatOrNull()
                val listeningScore = listening.text.toFloatOrNull()
                val writingScore = writing.text.toFloatOrNull()

                if (readingScore != null && listeningScore != null && writingScore != null) {
                    // 選択された日付から年を抽出
                    selectedYear = selectedDate.split("-")[0]
                    // 同一年のデータ数をカウント
                    val sameYearCount = scores.count { it.date.startsWith(selectedYear) }

                    if (sameYearCount >= 3) {
                        // 同一年データが3以上の場合
                        maxDataErrorDialog = true
                    } else {
                        // データを追加
                        viewModel.addScore(
                            selectedDate,
                            readingScore,
                            listeningScore,
                            writingScore
                        )
                    }
                } else {
                    showErrorDialog = true
                }
            }
        }) {
            Text("スコアを追加")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = {
            if (scores.isNotEmpty()) {
                showLatestScoreDeleteDialog = true
            }
        }) {
            Text("最新のスコアを削除")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = {
            showAllScoresDeleteDialog = true
        }) {
            Text("全てのスコアを削除")
        }

        Spacer(modifier = Modifier.height(4.dp))

        ScoreChart(scores)

        if (showLatestScoreDeleteDialog) {
            ConfirmDeleteDialog(
                message = "最新のスコアを削除しますか？",
                onConfirm = {
                    // 日付順で並び替え、最新のスコアを削除
                    val latestScore = scores.maxByOrNull { it.date } // 日付が最も新しいスコアを取得
                    latestScore?.let { viewModel.deleteScore(it) } // 存在すれば削除
                    showLatestScoreDeleteDialog = false
                },
                onDismiss = { showLatestScoreDeleteDialog = false }
            )
        }
        if (showAllScoresDeleteDialog) {
            ConfirmDeleteDialog(
                message = "全てのスコアを削除しますか？",
                onConfirm = {
                    viewModel.deleteAllScores()
                    showAllScoresDeleteDialog = false
                },
                onDismiss = { showAllScoresDeleteDialog = false }
            )
        }
        if (showDuplicateDateErrorDialog) {
            OkDialog(
                message = "同じ日付のスコアは登録できません。",
                onConfirm = { showDuplicateDateErrorDialog = false },
                onDismiss = { showDuplicateDateErrorDialog = false }
            )
        }
        if (showErrorDialog) {
            OkDialog(
                message = "入力されていない項目があります。",
                onConfirm = { showErrorDialog = false },
                onDismiss = { showErrorDialog = false }
            )
        }
        if (maxDataErrorDialog) {
            OkDialog(
                message = "同じ年で登録できるデータは３個までです。",
                onConfirm = { maxDataErrorDialog = false },
                onDismiss = { maxDataErrorDialog = false }
            )
        }
    }
}

@Composable
private fun SelectDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        R.style.CustomDatePickerTheme,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        }, year, month, day
    )
    datePickerDialog.datePicker.maxDate = calendar.timeInMillis
    Button(onClick = { datePickerDialog.show() }) {
        Text("受験日を選択する")
    }
}

@Composable
private fun ConfirmDeleteDialog(
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

@Composable
private fun OkDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { Text(message) }
    )
}

@Composable
private fun ScoreChart(scores: List<Score>) {
    val sortedScores = scores.sortedBy { it.date }

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
            .height(400.dp)
    ) { chart ->
        val entriesReading = sortedScores.mapIndexed { index, score -> Entry(index.toFloat(), score.reading) }
        val entriesListening = sortedScores.mapIndexed { index, score -> Entry(index.toFloat(), score.listening) }
        val entriesWriting = sortedScores.mapIndexed { index, score -> Entry(index.toFloat(), score.writing) }
        val dates = sortedScores.map { it.date }

        val dataSetReading = LineDataSet(entriesReading, "リーディングスコア").apply { color = android.graphics.Color.RED }
        val dataSetListening = LineDataSet(entriesListening, "リスニングスコア").apply { color = android.graphics.Color.BLUE }
        val dataSetWriting = LineDataSet(entriesWriting, "ライティングスコア").apply { color = android.graphics.Color.GREEN }

        dataSetReading.apply {
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.RED
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        dataSetListening.apply {
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.BLUE
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        dataSetWriting.apply {
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.GREEN
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(dataSetReading, dataSetListening, dataSetWriting)
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dates)
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 12f
            textColor = android.graphics.Color.BLACK
            granularity = 1f
        }
        chart.axisLeft.apply {
            axisMinimum = 0f
            textSize = 12f
            textColor = android.graphics.Color.BLACK
        }
        chart.axisRight.isEnabled = false
        chart.description.apply {
            isEnabled = true
            text = "英検一次の一年間のスコアの推移"
            textSize = 16f
            textColor = android.graphics.Color.CYAN
        }
        chart.animateX(250, com.github.mikephil.charting.animation.Easing.Linear)
        // 以下、作成した結果表示されているデータをタップ後に画面遷移するコード
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                TODO("Not yet implemented")
                e?.let {
//                    onSelection(it.x, it.y)
                }
            }
            override fun onNothingSelected() {
                TODO("Not yet implemented")

            }
        })
        chart.invalidate()
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreChartPreview() {
    CompareChartTheme {
        ScoreChart(
            scores = TODO()
        )
    }
}
