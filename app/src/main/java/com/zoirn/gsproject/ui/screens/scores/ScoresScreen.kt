package com.zoirn.gsproject.ui.screens.scores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.HomeworkGrade
import com.zoirn.gsproject.data.model.ScoreStats
import com.zoirn.gsproject.data.model.SubmissionItem
import com.zoirn.gsproject.ui.components.PullRefreshLayout
import com.zoirn.gsproject.ui.components.ShimmerProfilePlaceholder
import com.zoirn.gsproject.utils.EventLogger
import kotlinx.coroutines.launch

//экран с баллами студента (лабы + домашки)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(onBack: () -> Unit) {
    //стейт экрана
    var isLoading by remember { mutableStateOf(true) } //первичная загрузка
    var isRefreshing by remember { mutableStateOf(false) } //pull-to-refresh
    var stats by remember { mutableStateOf<ScoreStats?>(null) } //статистика
    var submissions by remember { mutableStateOf<List<SubmissionItem>>(emptyList()) } //сданные работы
    var homeworkGrades by remember { mutableStateOf<List<HomeworkGrade>>(emptyList()) } //оценки за домашки
    var error by remember { mutableStateOf<String?>(null) } //ошибка
    val scope = rememberCoroutineScope()

    //функция загрузки данных (используется и при старте и при обновлении)
    fun loadData() {
        scope.launch {
            error = null
            try {
                val resp = RetrofitClient.apiService.getStudentScores() //запрос к серверу
                if (resp.isSuccessful) {
                    val body = resp.body()
                    stats = body?.stats //общая статистика
                    submissions = body?.submissions ?: emptyList() //список работ
                    homeworkGrades = body?.homeworkGrades ?: emptyList() //оценки за домашки
                }
            } catch (e: Exception) {
                error = "Ошибка загрузки: ${e.message}"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    //логируем открытие экрана и загружаем данные при старте
    LaunchedEffect(Unit) {
        EventLogger.log("screen_view", "scores")
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои баллы", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        //пока грузится — шиммер-заглушка
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                ShimmerProfilePlaceholder()
            }
        } else {
            //после загрузки — список с pull-to-refresh
            PullRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; loadData() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                //баннер ошибки (если что-то пошло не так)
                error?.let { err ->
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                TextButton(onClick = { isLoading = true; loadData() }) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }

                //общая статистика и разбивка по предметам
                stats?.let { s ->
                    item { StatsOverview(s) } //общий средний балл
                    item { SubjectBreakdown(s) } //отдельно ИСРПО и РМП
                }

                //список сданных лабораторных работ
                if (submissions.isNotEmpty()) {
                    item {
                        Text("Сданные работы", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    //показываем только проверенные работы (с оценкой)
                    items(submissions.filter { it.score != null }) { sub ->
                        SubmissionCard(sub)
                    }
                }

                //список домашних работ с оценками
                if (homeworkGrades.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Домашние работы", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    items(homeworkGrades) { hw ->
                        HomeworkGradeCard(hw)
                    }
                }
            }
            }
        }
    }
}

//карточка с общей статистикой (средний, всего, количество работ)
@Composable
private fun StatsOverview(s: ScoreStats) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Общая статистика", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            //три числа в ряд
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Средний балл", "${s.averageScore}")
                StatItem("Всего баллов", "${s.totalScore}")
                StatItem("Сдано работ", "${s.completedWorks}")
            }
        }
    }
}

//один элемент статистики — большое число + подпись
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

//карточки с баллами отдельно по ИСРПО и РМП
@Composable
private fun SubjectBreakdown(s: ScoreStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        //карточка ИСРПО
        Card(
            Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA).copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("ИСРПО", fontWeight = FontWeight.Bold, color = Color(0xFF667EEA), fontSize = 14.sp)
                Text("${s.isrpoScore} баллов", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${s.isrpoCount} работ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.homeworkScore > 0) {
                    Text("ДЗ: ${s.homeworkScore} б.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        //карточка РМП
        Card(
            Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEC4899).copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("РМП", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899), fontSize = 14.sp)
                Text("${s.rmpScore} баллов", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${s.rmpCount} работ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

//карточка одной сданной лабораторной работы
@Composable
private fun SubmissionCard(sub: SubmissionItem) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                //название работы — берём из title или из названия репозитория
                Text(
                    sub.title ?: sub.githubRepoUrl?.substringAfterLast("/") ?: "Работа",
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    //дата сдачи (первые 10 символов — только дата без времени)
                    sub.submittedAt?.take(10)?.let {
                        Text("Сдано: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    //дата проверки
                    sub.checkedAt?.take(10)?.let {
                        Text("Проверено: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            //зелёный бейдж с оценкой справа
            sub.score?.let { score ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.2f))
                ) {
                    Text(
                        "$score",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80)
                    )
                }
            }
        }
    }
}

//карточка оценки за домашнюю работу
@Composable
private fun HomeworkGradeCard(hw: HomeworkGrade) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Домашка #${hw.homeworkNumber}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                //комментарий преподавателя (если есть)
                hw.comment?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            //оценка справа
            hw.score?.let { score ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.2f))
                ) {
                    Text("$score", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                }
            }
        }
    }
}
