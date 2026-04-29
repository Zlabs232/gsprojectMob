package com.zoirn.gsproject.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.BalanceResponse
import com.zoirn.gsproject.data.model.WeeklyTaskItem
import com.zoirn.gsproject.ui.components.PullRefreshLayout
import com.zoirn.gsproject.ui.components.ShimmerListPlaceholder
import com.zoirn.gsproject.utils.EventLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//экран заданий на неделю — список задач с прогресс-барами и кнопкой проверки
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTasksScreen(onBack: () -> Unit) {
    //стейт экрана
    var isLoading by remember { mutableStateOf(true) } //первичная загрузка
    var isRefreshing by remember { mutableStateOf(false) } //pull-to-refresh
    var tasks by remember { mutableStateOf<List<WeeklyTaskItem>>(emptyList()) } //список заданий
    var weekStart by remember { mutableStateOf("") } //дата начала недели
    var balance by remember { mutableStateOf<BalanceResponse?>(null) } //баланс коинов
    var checkCooldown by remember { mutableIntStateOf(0) } //кулдаун кнопки проверки (в секундах)
    var checkMessage by remember { mutableStateOf<String?>(null) } //сообщение после проверки
    var error by remember { mutableStateOf<String?>(null) } //ошибка
    val scope = rememberCoroutineScope()

    //загрузить задания и баланс
    fun loadData() {
        scope.launch {
            error = null
            try {
                //загружаем задания
                val tasksResp = RetrofitClient.apiService.getWeeklyTasks()
                if (tasksResp.isSuccessful) {
                    val body = tasksResp.body()
                    tasks = body?.tasks ?: emptyList()
                    weekStart = body?.weekStart ?: ""
                }
                //загружаем баланс
                val balResp = RetrofitClient.apiService.getGameBalance()
                if (balResp.isSuccessful) balance = balResp.body()
            } catch (e: Exception) {
                error = "Ошибка загрузки: ${e.message}"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    //логируем открытие и грузим данные
    LaunchedEffect(Unit) {
        EventLogger.log("screen_view", "weekly_tasks")
        loadData()
    }

    //таймер кулдауна — каждую секунду уменьшает счётчик на 1
    LaunchedEffect(checkCooldown) {
        if (checkCooldown > 0) {
            delay(1000) //ждём 1 секунду
            checkCooldown-- //уменьшаем счётчик
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Задания на неделю", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            //заглушка пока грузится
            Box(Modifier.fillMaxSize().padding(padding)) {
                ShimmerListPlaceholder()
            }
        } else {
            PullRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; loadData() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                //ошибка если есть
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

                //виджет баланса коинов
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🪙", fontSize = 24.sp)
                            Text("Баланс: ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text("${balance?.coins ?: 0.0}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("коинов", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                //подпись с датой начала текущей недели
                item {
                    Text(
                        "Неделя с $weekStart",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                //список карточек заданий
                items(tasks) { task ->
                    TaskCard(task)
                }

                //кнопка проверки выполнения заданий (с кулдауном 30 секунд)
                item {
                    Button(
                        onClick = {
                            if (checkCooldown > 0) return@Button //ещё кулдаун — ничего не делаем
                            scope.launch {
                                try {
                                    val resp = RetrofitClient.apiService.checkWeeklyTasks()
                                    if (resp.isSuccessful) {
                                        checkMessage = resp.body()?.message ?: "Проверено!"
                                        loadData() //обновляем список заданий
                                    } else {
                                        checkMessage = "Подождите немного..."
                                    }
                                } catch (e: Exception) {
                                    checkMessage = "Ошибка: ${e.message}"
                                }
                                checkCooldown = 30 //ставим кулдаун 30 секунд
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = checkCooldown == 0, //блокируем кнопку во время кулдауна
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        //текст кнопки: обычный или с обратным отсчётом
                        Text(
                            if (checkCooldown > 0) "Проверить (${checkCooldown}с)" else "Проверить выполнение",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                //зелёная карточка с сообщением после проверки
                checkMessage?.let { msg ->
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.15f))
                        ) {
                            Text(msg, Modifier.padding(14.dp), color = Color(0xFF4ADE80), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                //итоги недели — заработано коинов из максимума
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Награды", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            //считаем сколько заработал из максимума
                            val earned = tasks.filter { it.completed }.sumOf { it.reward }
                            val max = tasks.sumOf { it.reward }
                            Text(
                                "Заработано: ${earned} / ${max} коинов",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Максимум 2 коина в неделю", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    }
}

//карточка одного задания на неделю с прогресс-баром
@Composable
private fun TaskCard(task: WeeklyTaskItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            //выполненное задание — зеленоватый фон
            containerColor = if (task.completed)
                Color(0xFF4ADE80).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                //галочка если задание выполнено
                if (task.completed) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(22.dp))
                }
            }

            //описание что нужно сделать
            Text(task.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            //прогресс-бар + числа прогресса и награда
            val progress = if (task.target > 0) (task.progress.toFloat() / task.target).coerceIn(0f, 1f) else 0f
            Column {
                LinearProgressIndicator(
                    progress = { progress }, //0.0 - 1.0
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    //цвет полосы: зелёный если выполнено, акцентный если нет
                    color = if (task.completed) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round //закруглённые концы
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${task.progress} / ${task.target}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Награда: ${task.reward} коин", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
