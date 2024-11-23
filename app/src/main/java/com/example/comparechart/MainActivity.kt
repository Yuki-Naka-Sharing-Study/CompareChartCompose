package com.example.comparechart

import androidx.compose.ui.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.*
import com.example.comparechart.ui.theme.CompareChartTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

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

    var showLastScoreDialog by remember { mutableStateOf(false) }
    var showAllScoresDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showDuplicateDateErrorDialog by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") } // 選択された日付
    var reading by remember { mutableStateOf(TextFieldValue()) }
    var listening by remember { mutableStateOf(TextFieldValue()) }
    var writing by remember { mutableStateOf(TextFieldValue()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        DrumRollTypeDatePicker { date ->
            selectedDate = date
        }
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("日付 (年-月-日)") },
            readOnly = true
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
            if (scores.any { it.date == selectedDate }) {
                showDuplicateDateErrorDialog = true
            } else {
                val readingScore = reading.text.toFloatOrNull()
                val listeningScore = listening.text.toFloatOrNull()
                val writingScore = writing.text.toFloatOrNull()

                if (readingScore != null && listeningScore != null && writingScore != null) {
                    viewModel.addScore(
                        selectedDate,
                        readingScore,
                        listeningScore,
                        writingScore
                    )
                } else {
                    showErrorDialog = true
                }
            }
        }) {
            Text("スコアを追加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (scores.isNotEmpty()) {
                showLastScoreDialog = true
            }
        }) {
            Text("最後のスコアを削除")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            showAllScoresDialog = true
        }) {
            Text("全てのスコアを削除")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScoreChart(scores)

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
        if (showDuplicateDateErrorDialog) {
            ConfirmDeleteDialog(
                message = "同じ日付のスコアは登録できません。",
                onConfirm = { showDuplicateDateErrorDialog = false },
                onDismiss = { showDuplicateDateErrorDialog = false }
            )
        }
        if (showErrorDialog) {
            ConfirmDeleteDialog(
                message = "入力が不正です。正しい数値を入力してください。",
                onConfirm = { showErrorDialog = false },
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}

@Composable
fun DrumRollTypeDatePicker(onDateSelected: (String) -> Unit) {
    var isPickerVisible by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("日付を選択") }

    Box(contentAlignment = Alignment.Center) {
        Column() {
            Button(onClick = { isPickerVisible = true }) {
                Text(text = selectedDate)
            }
        }

        if (isPickerVisible) {
            DatePickerDialog(
                onDismiss = { isPickerVisible = false },
                onDateSelected = { year, month, day ->
                    selectedDate = "$year 年 $month 月 $day 日"
                    onDateSelected(selectedDate)
                    isPickerVisible = false
                }
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (1900..2100).toList()
    val months = (1..12).toList()
    val days = (1..31).toList()

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(1) }
    var selectedDay by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { onDateSelected(selectedYear, selectedMonth, selectedDay) }) {
                    Text("確定")
                }
            }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("日付を選択")
            }
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScrollPicker(
                    items = years,
                    selectedItem = selectedYear,
                    onItemSelected = { selectedYear = it },
                    highlightColor = Color.Red
                )
                ScrollPicker(
                    items = months,
                    selectedItem = selectedMonth,
                    onItemSelected = { selectedMonth = it },
                    highlightColor = Color.Green
                )
                ScrollPicker(
                    items = days,
                    selectedItem = selectedDay,
                    onItemSelected = { selectedDay = it },
                    highlightColor = Color.Blue
                )
            }
        }
    )
}

@Composable
fun <T> ScrollPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    highlightColor: Color
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 初期位置にスクロール
    LaunchedEffect(items, selectedItem) {
        val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        listState.scrollToItem(initialIndex)
    }

    // スクロール停止時の選択更新
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling) {
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val firstVisibleItemOffset = listState.firstVisibleItemScrollOffset
                    val visibleItems = listState.layoutInfo.visibleItemsInfo

                    if (visibleItems.isNotEmpty()) {
                        val centerIndex = if (firstVisibleItemOffset > (visibleItems.first().size / 2)) {
                            firstVisibleIndex + 1
                        } else {
                            firstVisibleIndex
                        }.coerceIn(0, items.lastIndex)

                        onItemSelected(items[centerIndex])
                    }
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .height(150.dp)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size) { index ->
            val item = items[index]
            Text(
                text = item.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                            onItemSelected(item)
                        }
                    },
                style = if (item == selectedItem) {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                }
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
        val dataSetWriting = LineDataSet(entriesWriting, "ライティングスコア").apply { color = android.graphics.Color.GREEN }

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
    CompareChartTheme {
        ScoreChart(
            scores = TODO()
        )
    }
}