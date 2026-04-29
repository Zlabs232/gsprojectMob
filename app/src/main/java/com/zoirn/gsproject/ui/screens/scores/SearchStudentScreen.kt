package com.zoirn.gsproject.ui.screens.scores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.StudentSearchResult
import com.zoirn.gsproject.utils.EventLogger
import kotlinx.coroutines.launch

//экран поиска студентов по ФИО или github логину
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStudentScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") } //текст поискового запроса
    var results by remember { mutableStateOf<List<StudentSearchResult>>(emptyList()) } //результаты поиска
    var isSearching by remember { mutableStateOf(false) } //идёт ли поиск
    var searched by remember { mutableStateOf(false) } //был ли хоть один поиск (для "ничего не найдено")
    var error by remember { mutableStateOf<String?>(null) } //ошибка
    val scope = rememberCoroutineScope()

    //логируем открытие экрана
    LaunchedEffect(Unit) { EventLogger.log("screen_view", "search_student") }

    //функция выполнения поиска
    fun doSearch() {
        scope.launch {
            isSearching = true
            searched = true
            error = null
            try {
                val resp = RetrofitClient.apiService.searchStudent(query) //запрос к серверу
                if (resp.isSuccessful) {
                    results = resp.body()?.students ?: emptyList()
                } else {
                    error = "Ошибка: ${resp.code()}"
                }
            } catch (e: Exception) {
                error = "Ошибка: ${e.message}"
            }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск студента", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            //поле ввода поискового запроса
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("ФИО или GitHub логин...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, //иконка поиска слева
                trailingIcon = {
                    //кнопка поиска справа — появляется когда введено минимум 2 символа
                    if (query.length >= 2) {
                        IconButton(onClick = { doSearch() }) {
                            Icon(Icons.Default.Search, "Искать", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )

            //состояния экрана: загрузка / ошибка / пусто / результаты
            if (isSearching) {
                //крутилка во время поиска
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (error != null) {
                //карточка с ошибкой и кнопкой "повторить"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { doSearch() }) {
                            Text("Повторить")
                        }
                    }
                }
            } else if (searched && results.isEmpty()) {
                //если поиск был но ничего не нашли
                Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            } else {
                //список найденных студентов
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results) { student ->
                        StudentCard(student)
                    }
                }
            }
        }
    }
}

//карточка найденного студента с аватаркой и статистикой
@Composable
private fun StudentCard(student: StudentSearchResult) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            //шапка: аватарка + имя + группа
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                //аватарка из гитхаба или сгенерированная из имени
                AsyncImage(
                    model = student.avatarUrl ?: "https://ui-avatars.com/api/?name=${student.fullName}&background=8b5cf6&color=fff",
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape) //круглая аватарка
                )
                Column {
                    Text(student.fullName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(student.groupName ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            //мини-статистика по баллам (если есть данные)
            student.stats?.let { s ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MiniStat("Всего", "${s.totalScore}")
                    MiniStat("ИСРПО", "${s.isrpoScore}")
                    MiniStat("РМП", "${s.rmpScore}")
                    MiniStat("Работ", "${s.completedWorks}")
                }
            }
        }
    }
}

//маленький элемент статистики — число + подпись
@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
