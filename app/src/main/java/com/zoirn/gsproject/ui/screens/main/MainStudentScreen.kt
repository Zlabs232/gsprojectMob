package com.zoirn.gsproject.ui.screens.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zoirn.gsproject.data.model.RepoItem
import com.zoirn.gsproject.ui.components.PullRefreshLayout
import com.zoirn.gsproject.ui.components.ShimmerMainPlaceholder
import com.zoirn.gsproject.utils.AppTheme
import com.zoirn.gsproject.utils.EventLogger
import com.zoirn.gsproject.utils.ThemePreferences
import kotlinx.coroutines.launch

//главный экран студента — репозитории, кнопки, баланс коинов
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudentScreen(
    onNavigateToGame: () -> Unit, //переход на экран игры
    onNavigateToScores: () -> Unit, //переход на баллы
    onNavigateToSearch: () -> Unit, //переход на поиск студентов
    onNavigateToWeeklyTasks: () -> Unit, //переход на задания недели
    onNavigateToHomework: () -> Unit, //переход на домашние работы
    onNavigateToNotifications: () -> Unit, //переход на уведомления
    onLogout: () -> Unit, //выход из аккаунта
    viewModel: MainViewModel = viewModel() //вьюмодель главного экрана
) {
    //подписка на стейт вьюмодели
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed) //состояние бокового меню
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentTheme by remember { mutableStateOf(AppTheme.DARK) } //текущая тема

    //логируем открытие главного экрана
    LaunchedEffect(Unit) { EventLogger.log("screen_view", "main") }

    //подписываемся на изменения темы из DataStore
    val themeFlow = remember { ThemePreferences.getTheme(context) }
    val theme by themeFlow.collectAsState(initial = AppTheme.DARK)
    LaunchedEffect(theme) { currentTheme = theme } //обновляем локальный стейт при изменении

    //ModalNavigationDrawer — боковое меню (шторка слева)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                profile = state.profile,
                balance = state.balance,
                streakDays = state.balance?.streakDays ?: 0,
                streakBonus = state.balance?.streakBonus ?: 1.0,
                onLabClick = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) //открываем ссылку в браузере
                },
                onImportantClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://lmsgsproject.ru/frontend/important-info.html"))
                    )
                },
                onLogout = {
                    viewModel.logout() //чистим сессию
                    onLogout() //переходим на экран логина
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    unreadCount = state.unreadNotifications, //количество непрочитанных уведомлений
                    currentTheme = currentTheme,
                    onMenuClick = { scope.launch { drawerState.open() } }, //открыть боковое меню
                    onWeeklyTasksClick = onNavigateToWeeklyTasks,
                    onThemeToggle = {
                        //переключить тему тёмная ↔ розовая
                        scope.launch {
                            val newTheme = if (currentTheme == AppTheme.DARK) AppTheme.PINK else AppTheme.DARK
                            ThemePreferences.setTheme(context, newTheme)
                        }
                    },
                    onNotificationsClick = onNavigateToNotifications
                )
            }
        ) { padding ->
            if (state.isLoading) {
                //пока грузится — шиммер-заглушка
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ShimmerMainPlaceholder()
                }
            } else {
                PullRefreshLayout(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    //баннер ошибки если что-то не загрузилось
                    state.error?.let { err ->
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
                                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.loadAll() }) {
                                        Text("Повторить", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    //шапка раздела репозиториев с фильтрами по предмету
                    item { ReposSectionHeader(state, viewModel) }

                    //строка поиска по репозиториям
                    item {
                        OutlinedTextField(
                            value = state.repoSearchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Поиск репозитория...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }

                    //список репозиториев или сообщение если пусто
                    if (state.repos.isEmpty()) {
                        item {
                            Text(
                                "Нет репозиториев по фильтру",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        //каждый репозиторий — отдельная карточка, key для оптимизации списка
                        items(state.repos, key = { it.name }) { repo ->
                            RepoCard(repo, context)
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    //сетка кнопок действий (баллы, поиск, игра, домашки)
                    item { ActionsGrid(
                        onScoresClick = onNavigateToScores,
                        onSearchClick = onNavigateToSearch,
                        onGameClick = onNavigateToGame,
                        onHomeworkClick = onNavigateToHomework
                    ) }

                    //блок с балансом коинов
                    item { CoinBalanceBlock(state.balance) }

                    item { Spacer(Modifier.height(16.dp)) }
                }
                }
            }
        }
    }
}

//верхняя панель (TopAppBar) главного экрана
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    unreadCount: Int, //количество непрочитанных уведомлений для бейджа
    currentTheme: AppTheme,
    onMenuClick: () -> Unit, //открыть боковое меню
    onWeeklyTasksClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Главная панель", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
        },
        actions = {
            //кнопка задания на неделю
            IconButton(onClick = onWeeklyTasksClick) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Задания на неделю")
            }
            //переключатель темы — иконка меняется в зависимости от активной темы
            IconButton(onClick = onThemeToggle) {
                Icon(
                    if (currentTheme == AppTheme.DARK) Icons.Default.DarkMode else Icons.Default.Favorite,
                    contentDescription = "Сменить тему",
                    tint = if (currentTheme == AppTheme.PINK) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface
                )
            }
            //иконка уведомлений с бейджом (цифра непрочитанных)
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) } //красный кружок с цифрой
                    }
                }
            ) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "Уведомления")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

//содержимое бокового меню (шторки)
@Composable
private fun DrawerContent(
    profile: com.zoirn.gsproject.data.model.ProfileResponse?,
    balance: com.zoirn.gsproject.data.model.BalanceResponse?,
    streakDays: Int, //дней подряд
    streakBonus: Double, //бонусный множитель
    onLabClick: (String) -> Unit, //клик по ссылке на лабораторные
    onImportantClick: () -> Unit, //кнопка "Важно!"
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp)
                .width(280.dp) //фиксированная ширина меню
        ) {
            //шапка меню — аватарка + имя + группа
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //если нет аватарки — генерируем из имени через ui-avatars.com
                val avatarUrl = profile?.avatarUrl
                    ?: "https://ui-avatars.com/api/?name=${profile?.firstName ?: "U"}&background=8b5cf6&color=fff&size=120"
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape) //круглая аватарка
                )
                Column {
                    //полное имя (Фамилия Имя)
                    Text(
                        text = buildString {
                            append(profile?.lastName ?: "")
                            append(" ")
                            append(profile?.firstName ?: "")
                        }.trim().ifEmpty { "Студент" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = profile?.groupName ?: "",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            //карточка стрика — сколько дней подряд заходит в приложение
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f) //жёлтый оттенок
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFF59E0B).copy(alpha = 0.3f), Color(0xFFF59E0B).copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔥", fontSize = 18.sp)
                            Text(
                                "$streakDays", //количество дней
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                "дней подряд",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        //бонусный множитель за стрик
                        Text(
                            "×${streakBonus.toInt()} 🎁/день",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    //прогресс-бар стрика (шкала до 100 дней)
                    LinearProgressIndicator(
                        progress = { (streakDays / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF8B5CF6), //фиолетовая полоска
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            //секция ссылок на лабораторные работы
            Text(
                "Лабораторные работы",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            //ссылки на методички по предметам
            LabLink("Лабы ИСРПО") {
                onLabClick("https://paltosik92.github.io/it-labs/labs/fullstack/index.html#")
            }
            LabLink("Лабы РМП (Kotlin)") {
                onLabClick("https://paltosik92.github.io/it-labs/labs/programming/kotlin/index.html")
            }
            LabLink("Лабы РМП (Android Studio)") {
                onLabClick("https://paltosik92.github.io/it-labs/labs/mobile/index.html")
            }

            Spacer(Modifier.height(8.dp))

            //кнопка "Важно!" — красно-жёлтый градиент, ведёт на страницу важной инфы
            Button(
                onClick = onImportantClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Важно!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f)) //пушим кнопку выхода вниз

            //кнопка выхода — красный текст
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text("Выйти", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

//кнопка-ссылка на лабораторные работы в боковом меню
@Composable
private fun LabLink(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
    }
}

//шапка раздела репозиториев с чипами выбора предмета
@Composable
private fun ReposSectionHeader(state: MainUiState, viewModel: MainViewModel) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("GitHub репозитории", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Text("с 01.01.2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) //начало отсчёта
        }

        Spacer(Modifier.height(12.dp))

        //чипы выбора предмета
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubjectChip("ИСРПО", state.selectedSubject == "ISRPO") {
                viewModel.setSubject("ISRPO")
            }
            SubjectChip("РМП", state.selectedSubject == "RMP") {
                viewModel.setSubject("RMP")
            }
        }

        //подкатегории РМП — показываются только если выбрано РМП
        if (state.selectedSubject == "RMP") {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectChip("Kotlin", state.selectedRmpSub == "Kotlin") {
                    viewModel.setRmpSub("Kotlin")
                }
                SubjectChip("Android Studio", state.selectedRmpSub == "Androidstudio") {
                    viewModel.setRmpSub("Androidstudio")
                }
            }
        }
    }
}

//один чип для выбора предмета
@Composable
private fun SubjectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), //подсветка выбранного
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

//карточка репозитория — нажатие открывает репу в браузере
@Composable
private fun RepoCard(repo: RepoItem, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.url))) //открываем в браузере
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //название репы — обрезается если длинное
                Text(
                    text = repo.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                //зелёный бейдж с оценкой (если преподаватель выставил)
                if (repo.score != null) {
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4ADE80).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "${repo.score}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            //вторая строка — язык, дата обновления, статус проверки
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (repo.language != null) {
                    Text(repo.language, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                //берём только дату (первые 10 символов)
                repo.updatedAt?.take(10)?.let { date ->
                    Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                //зелёная метка если работа проверена
                if (repo.checked) {
                    Text("Проверено", fontSize = 12.sp, color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

//сетка кнопок действий — 2 столбца, 3 ряда (6 кнопок)
@Composable
private fun ActionsGrid(
    onScoresClick: () -> Unit,
    onSearchClick: () -> Unit,
    onGameClick: () -> Unit,
    onHomeworkClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(icon = Icons.Default.BarChart, label = "Просмотр баллов", modifier = Modifier.weight(1f), onClick = onScoresClick)
            ActionButton(icon = Icons.Default.CalendarMonth, label = "Аукцион\n(в разработке)", modifier = Modifier.weight(1f), onClick = {})
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(icon = Icons.Default.Search, label = "Поиск студента", modifier = Modifier.weight(1f), onClick = onSearchClick)
            ActionButton(icon = Icons.AutoMirrored.Filled.ShowChart, label = "Оценки\nи аналитика", modifier = Modifier.weight(1f), onClick = onScoresClick)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(icon = Icons.Default.SportsEsports, label = "Игра", modifier = Modifier.weight(1f), onClick = onGameClick)
            ActionButton(icon = Icons.Default.Checklist, label = "Домашние работы", modifier = Modifier.weight(1f), onClick = onHomeworkClick)
        }
    }
}

//одна кнопка из сетки — иконка + подпись
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2 //максимум 2 строки текста
            )
        }
    }
}

//блок с балансом коинов и кнопкой обмена
@Composable
private fun CoinBalanceBlock(balance: com.zoirn.gsproject.data.model.BalanceResponse?) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f) //фиолетовый оттенок
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = Brush.linearGradient(
                listOf(Color(0xFF8B5CF6).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //строка с иконкой и суммой коинов
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🪙", fontSize = 18.sp)
                Text("Баланс: ", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${balance?.coins ?: 0.0}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B), //жёлтый цвет для коинов
                    fontSize = 18.sp
                )
                Text("коинов", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(12.dp))

            //кнопка обмена коинов на баллы (TODO: открыть модалку обмена)
            Button(
                onClick = { /* TODO: exchange coins modal */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))), //жёлто-оранжевый градиент
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Обменять коины на баллы", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
