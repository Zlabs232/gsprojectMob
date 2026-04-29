package com.zoirn.gsproject.ui.screens.homework

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.HomeworkItem
import com.zoirn.gsproject.data.model.HomeworkSubmitRequest
import com.zoirn.gsproject.ui.components.PullRefreshLayout
import com.zoirn.gsproject.ui.components.ShimmerListPlaceholder
import com.zoirn.gsproject.utils.EventLogger
import kotlinx.coroutines.launch

//экран домашних работ — список сданных + форма отправки
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(onBack: () -> Unit) {
    //стейт экрана
    var isLoading by remember { mutableStateOf(true) } //первичная загрузка
    var isRefreshing by remember { mutableStateOf(false) } //pull-to-refresh
    var homework by remember { mutableStateOf<List<HomeworkItem>>(emptyList()) } //список домашек
    var showSubmitForm by remember { mutableStateOf(false) } //показывать ли форму сдачи
    //поля формы сдачи домашки
    var submitNumber by remember { mutableStateOf("1") } //номер домашки (1-4)
    var submitUrl by remember { mutableStateOf("") } //ссылка на сертификат степика
    var submitComment by remember { mutableStateOf("") } //комментарий
    var submitError by remember { mutableStateOf<String?>(null) } //ошибка валидации формы
    var submitSuccess by remember { mutableStateOf(false) } //успешно ли отправлено
    var isSubmitting by remember { mutableStateOf(false) } //идёт ли отправка
    var error by remember { mutableStateOf<String?>(null) } //ошибка загрузки
    val scope = rememberCoroutineScope()

    //загрузить список домашних работ
    fun loadHomework() {
        scope.launch {
            error = null
            try {
                val resp = RetrofitClient.apiService.getHomework()
                if (resp.isSuccessful) {
                    homework = resp.body()?.homework ?: emptyList()
                }
            } catch (e: Exception) {
                error = "Ошибка загрузки: ${e.message}"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    //логируем открытие и загружаем список
    LaunchedEffect(Unit) {
        EventLogger.log("screen_view", "homework")
        loadHomework()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Домашние работы", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    //кнопка "+" в тулбаре — показать/скрыть форму сдачи
                    IconButton(onClick = { showSubmitForm = !showSubmitForm }) {
                        Icon(Icons.Default.Add, "Сдать ДЗ", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            //пока грузится — заглушка
            Box(Modifier.fillMaxSize().padding(padding)) {
                ShimmerListPlaceholder()
            }
        } else {
            PullRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; loadHomework() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                //баннер ошибки загрузки
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
                                TextButton(onClick = { isLoading = true; loadHomework() }) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }

                //форма сдачи домашней работы (показывается по кнопке "+")
                if (showSubmitForm) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Сдать домашнюю работу", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface)

                                //чипы выбора номера домашки (1, 2, 3, 4)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    (1..4).forEach { num ->
                                        FilterChip(
                                            selected = submitNumber == num.toString(), //активный = выбранный
                                            onClick = { submitNumber = num.toString() },
                                            label = { Text("ДЗ $num") }
                                        )
                                    }
                                }

                                //поле для ссылки на сертификат степика
                                OutlinedTextField(
                                    value = submitUrl,
                                    onValueChange = { submitUrl = it },
                                    placeholder = { Text("Ссылка на сертификат Stepik") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Uri),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    )
                                )

                                //поле для комментария (необязательное)
                                OutlinedTextField(
                                    value = submitComment,
                                    onValueChange = { submitComment = it },
                                    placeholder = { Text("Комментарий (необязательно)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    )
                                )

                                //ошибка валидации формы
                                submitError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                }

                                //сообщение об успешной отправке
                                if (submitSuccess) {
                                    Text("Домашка отправлена!", color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
                                }

                                //кнопка отправить
                                Button(
                                    onClick = {
                                        //простая валидация — нужна ссылка
                                        if (submitUrl.isBlank()) {
                                            submitError = "Укажите ссылку на сертификат"
                                            return@Button
                                        }
                                        submitError = null
                                        isSubmitting = true
                                        scope.launch {
                                            try {
                                                //отправляем домашку на сервер
                                                val resp = RetrofitClient.apiService.submitHomework(
                                                    HomeworkSubmitRequest(
                                                        homeworkNumber = submitNumber.toInt(),
                                                        stepikCertificateUrl = submitUrl.trim(),
                                                        comment = submitComment.trim().ifEmpty { null } //пустой = null
                                                    )
                                                )
                                                if (resp.isSuccessful) {
                                                    submitSuccess = true
                                                    submitUrl = "" //очищаем поля
                                                    submitComment = ""
                                                    EventLogger.log("submit_homework", "hw_$submitNumber") //логируем событие
                                                    loadHomework() //обновляем список
                                                } else {
                                                    submitError = "Ошибка: ${resp.code()}"
                                                }
                                            } catch (e: Exception) {
                                                submitError = "Ошибка: ${e.message}"
                                            }
                                            isSubmitting = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isSubmitting, //блокируем пока отправляется
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    //крутилка или текст в кнопке
                                    if (isSubmitting) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Отправить", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                //список уже сданных домашних работ
                if (homework.isEmpty()) {
                    item {
                        Text("Нет сданных работ", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp))
                    }
                } else {
                    item {
                        Text("Сданные работы", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    items(homework) { hw -> HomeworkCard(hw) }
                }
            }
            }
        }
    }
}

//карточка одной домашней работы
@Composable
private fun HomeworkCard(hw: HomeworkItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Домашка #${hw.homeworkNumber}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                //цвет и текст статуса
                val statusColor = when (hw.status) {
                    "graded" -> Color(0xFF4ADE80) //зелёный — оценено
                    "submitted" -> Color(0xFFF59E0B) //жёлтый — на проверке
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val statusText = when (hw.status) {
                    "graded" -> "Оценено"
                    "submitted" -> "На проверке"
                    else -> hw.status //неизвестный статус — показываем как есть
                }
                Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)

                //дата сдачи (только дата без времени)
                hw.submittedAt?.take(10)?.let {
                    Text("Сдано: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                //комментарий преподавателя (если есть)
                hw.comment?.let {
                    Text("Комментарий: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            //бейдж с оценкой справа
            hw.score?.let { score ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.2f))
                ) {
                    Text(
                        "$score",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), fontSize = 16.sp
                    )
                }
            }
        }
    }
}
