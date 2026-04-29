package com.zoirn.gsproject.ui.screens.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoirn.gsproject.data.model.SpecialPrizeItem
import com.zoirn.gsproject.data.model.SpinHistoryItem
import com.zoirn.gsproject.utils.EventLogger

//цены продажи призов за коины (id приза → цена в коинах)
private val SELL_PRICES = mapOf(
    "lab_home" to 0.5,
    "no_defense" to 1.0,
    "lab_min" to 1.0,
    "partial_lab" to 0.5,
)

//главный экран игры
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel() //создаём или переиспользуем вьюмодель
) {
    //подписываюсб на состояние (обновляется автоматически при изменении)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    //логирую открытие экрана
    LaunchedEffect(Unit) { EventLogger.log("screen_view", "game") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Игра-рулетка", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    //кнопка "Призы" — открывает диалог с особыми призами
                    TextButton(onClick = { viewModel.showPrizes() }) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(4.dp))
                        Text("Призы", color = MaterialTheme.colorScheme.onSurface)
                    }
                    //кнопка "История" — открывает список прошлых прокрутов
                    TextButton(onClick = { viewModel.showHistory() }) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("История", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            //загрузка баланса — показываем крутилку
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            //основной контент
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item { BalanceSection(state) } //баланс коинов и фриспинов
                item { DailyFreespinButton(state, viewModel) } //кнопка ежедневного фриспина
                item { CaseBox(state, viewModel) } //кейс для прокрута
                item { RulesSection() } //правила и вероятности призов
            }
        }
    }

    //диалог с результатом прокрута
    if (state.showResult && state.spinResult != null) {
        SpinResultDialog(state, viewModel)
    }

    //диалог истории прокрутов
    if (state.showHistory) {
        HistoryDialog(state, viewModel)
    }

    //диалог со списком призов
    if (state.showPrizes) {
        PrizesDialog(state, viewModel)
    }

    //автоматически скрываю ошибку через 3 секунды
    if (state.error != null) {
        LaunchedEffect(state.error) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissError()
        }
    }

    //автоматически скрываю сообщение о фриспине через 3 секунды
    if (state.freespinMessage != null) {
        LaunchedEffect(state.freespinMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissFreespinMessage()
        }
    }

    //снэкбары поверх всего (ошибки и сообщения о фриспине)
    Box(modifier = Modifier.fillMaxSize()) {
        //снэкбар ошибки — красный
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text(error, color = Color.White)
            }
        }
        //снэкбар фриспина — зелёный
        state.freespinMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Color(0xFF4ADE80)
            ) {
                Text(msg, color = Color.White)
            }
        }
    }
}

//секция с балансом — две карточки (коины и фриспины) рядом
@Composable
private fun BalanceSection(state: GameUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //карточка с коинами
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f) //жёлтый оттенок
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🪙", fontSize = 28.sp)
                Column {
                    Text("Коины", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.balance?.coins ?: 0.0}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        //карточка с фриспинами
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f) //фиолетовый оттенок
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎁", fontSize = 28.sp)
                Column {
                    Text("Фриспины", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.balance?.freespins ?: 0}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
        }
    }
}

//кнопка ежедневного фриспина — зелёная, блокируется если уже получен
@Composable
private fun DailyFreespinButton(state: GameUiState, viewModel: GameViewModel) {
    val available = state.balance?.dailyFreespinAvailable ?: true //доступен ли фриспин сегодня
    Button(
        onClick = { viewModel.claimDailyFreespin() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = available, //заблокирована если уже получен
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981), //зелёный
            disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.3f) //серо-зелёный если недоступно
        )
    ) {
        Text(
            if (available) "🎁 Забрать ежедневный фриспин" else "Фриспин уже получен сегодня",
            fontWeight = FontWeight.SemiBold
        )
    }
}

//кейс — главный элемент игры, нажатие = прокрут рулетки
@Composable
private fun CaseBox(state: GameUiState, viewModel: GameViewModel) {
    val isSpinning = state.isSpinning

    //анимация вращения во время прокрута
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing) //1 оборот в секунду
        ),
        label = "rotation"
    )

    //пульсация кейса в покое
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse //туда-обратно
        ),
        label = "pulse"
    )

    //можно крутить если не идёт анимация и есть ресурсы
    val canSpin = !isSpinning &&
            ((state.balance?.coins ?: 0.0) >= 1 || (state.balance?.freespins ?: 0) >= 1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .then(if (!isSpinning) Modifier.scale(pulseScale) else Modifier) //пульсация только в покое
            .clickable(enabled = canSpin) { viewModel.spin() }, //нажатие = крутим
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    //радиальный градиент — центр слегка подсвечен акцентным цветом
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSpinning) {
                //анимация прокрута — полоска с призами
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SlotStripAnimation(state.spinResult?.visualSequence) //анимированные слоты

                    Text(
                        "Крутим...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                //в покое — коробка с текстом "нажми"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📦", fontSize = 64.sp) //большая коробка
                    Text(
                        "Нажми, чтобы открыть!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    //подпись стоимости прокрута
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎁 1 фриспин", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("или", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("🪙 1 коин", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    //сообщение если нет ресурсов
                    if (!canSpin && !isSpinning) {
                        Text(
                            "Недостаточно средств",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

//анимация слотов во время прокрута — горизонтальная лента призов
@Composable
private fun SlotStripAnimation(visualSequence: List<com.zoirn.gsproject.data.model.PrizeItem>?) {
    val prizes = visualSequence ?: return //нет последовательности — ничего не показываем

    //анимация позиции ленты (от 0 до конца с замедлением)
    val offsetAnim = remember { Animatable(0f) }
    val itemCount = prizes.size

    LaunchedEffect(prizes) {
        offsetAnim.animateTo(
            targetValue = (itemCount - 5).toFloat(), //до предпоследнего элемента
            animationSpec = tween(
                durationMillis = 2800, //2.8 секунды
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f) //плавное торможение
            )
        )
    }

    //текущий индекс элемента по позиции анимации
    val currentIndex = offsetAnim.value.toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))

    //горизонтальный список призов (не прокручиваемый — управляется анимацией)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        userScrollEnabled = false //запрещаю ручной скролл
    ) {
        val startIndex = currentIndex.coerceAtLeast(0)
        val endIndex = (currentIndex + 5).coerceAtMost(itemCount) //показываю 5 призов

        //рисую карточки призов от startIndex до endIndex
        items(prizes.subList(startIndex.coerceAtMost(itemCount), endIndex.coerceAtMost(itemCount))) { prize ->
            Card(
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(prize.icon, fontSize = 32.sp) //иконка приза
                }
            }
        }
    }
}

//диалог с результатом прокрута
@Composable
private fun SpinResultDialog(state: GameUiState, viewModel: GameViewModel) {
    val prize = state.spinResult?.prize ?: return //нет приза — диалог не показываем

    Dialog(onDismissRequest = { viewModel.dismissResult() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                //анимированная иконка приза — пульсирует
                val scale by rememberInfiniteTransition(label = "prize").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Text(
                    prize.icon,
                    fontSize = 64.sp,
                    modifier = Modifier.scale(scale) //увеличиваем/уменьшаем
                )

                //название приза
                Text(
                    prize.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                //сколько коинов упало (если это коины)
                if (prize.type == "coins" && prize.value > 0) {
                    Text(
                        "+${prize.value.toInt()} коин(ов)",
                        fontSize = 16.sp,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                //специальная плашка для редкого прокрута
                if (state.spinResult?.isRareSpin == true) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            "Редкий слот!",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                //кнопка закрыть диалог
                Button(
                    onClick = { viewModel.dismissResult() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Продолжить", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

//диалог истории прокрутов
@Composable
private fun HistoryDialog(state: GameUiState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = { viewModel.hideHistory() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f), //занимает 70% высоты экрана
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                //заголовок с кнопкой закрыть
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "История прокрутов",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.hideHistory() }) {
                        Icon(Icons.Default.Close, "Закрыть")
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (state.historyItems.isEmpty()) {
                    //пустая история
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет истории", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    //список записей истории
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.historyItems) { item ->
                            HistoryItemCard(item)
                        }
                    }
                }
            }
        }
    }
}

//одна карточка в истории прокрутов
@Composable
private fun HistoryItemCard(item: SpinHistoryItem) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.prizeIcon, fontSize = 28.sp) //иконка приза
            Column(modifier = Modifier.weight(1f)) {
                Text(item.prizeName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                //дата — убираем T (разделитель дата/время в ISO формате)
                Text(
                    item.createdAt.take(16).replace("T", " "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            //бейдж если использовал фриспин
            if (item.usedFreespin) {
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        "🎁",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

//диалог с особыми призами студента
@Composable
private fun PrizesDialog(state: GameUiState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = { viewModel.hidePrizes() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f), //75% высоты экрана
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                //заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Мои призы", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { viewModel.hidePrizes() }) {
                        Icon(Icons.Default.Close, "Закрыть")
                    }
                }

                Spacer(Modifier.height(8.dp))

                //вкладки: активные и использованные
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.prizesTab == "active",
                        onClick = { viewModel.setPrizesTab("active") },
                        label = { Text("Активные") },
                        leadingIcon = { Text("🎁", fontSize = 14.sp) }
                    )
                    FilterChip(
                        selected = state.prizesTab == "used",
                        onClick = { viewModel.setPrizesTab("used") },
                        label = { Text("Использованные") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                //показываю активные или использованные в зависимости от вкладки
                val prizes = if (state.prizesTab == "active") state.activePrizes else state.usedPrizes

                if (prizes.isEmpty()) {
                    //пустая вкладка
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.prizesTab == "active") "Нет активных призов" else "Нет использованных призов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    //список призов
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(prizes) { prize ->
                            PrizeItemCard(
                                prize = prize,
                                isActive = state.prizesTab == "active",
                                onSell = { viewModel.sellPrize(prize.id) }, //продать за коины
                                onRequest = { viewModel.requestPrize(prize.id) } //применить
                            )
                        }
                    }
                }
            }
        }
    }
}

//карточка одного особого приза с кнопками действий
@Composable
private fun PrizeItemCard(
    prize: SpecialPrizeItem,
    isActive: Boolean, //активный (можно применить) или использованный
    onSell: () -> Unit, //продать за коины
    onRequest: () -> Unit //применить у препода
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(prize.prizeIcon, fontSize = 28.sp) //иконка
                Column(modifier = Modifier.weight(1f)) {
                    Text(prize.prizeName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(prize.createdAt.take(10), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) //дата получения
                }
            }

            //кнопки "Применить" и "Trade" — только для активных призов
            if (isActive) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //кнопка "Применить" — отправляет заявку преподу
                    OutlinedButton(
                        onClick = onRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Применить", fontSize = 13.sp)
                    }

                    //кнопка "Trade" — продать за коины (только если есть цена)
                    val sellPrice = SELL_PRICES[prize.prizeId]
                    if (sellPrice != null) {
                        Button(
                            onClick = onSell,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B) //жёлтая кнопка
                            )
                        ) {
                            Text("Trade ${sellPrice}🪙", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

//секция с правилами и вероятностями призов
@Composable
private fun RulesSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Правила и вероятности",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            //список всех призов с иконкой, названием и вероятностью
            val prizes = listOf(
                Triple("❌", "Ничего", "35%"),
                Triple("🎁", "Фриспин", "15%"),
                Triple("📋", "Сдать только сделанное", "10%"),
                Triple("🪙", "+1 коин", "10%"),
                Triple("💰", "+2 коина", "7%"),
                Triple("🏠", "Лаба дома без видео", "6%"),
                Triple("🛡️", "Без защиты (Android)", "5%"),
                Triple("💎", "+3 коина", "4%"),
                Triple("⭐", "Закрыть лабу на мин.", "3%"),
                Triple("💸", "+5 коинов", "3%"),
                Triple("👑", "Закрыть лабу на макс.", "2%"),
            )

            //рисую строку для каждого приза
            prizes.forEach { (icon, name, prob) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 18.sp)
                        Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    //вероятность справа — выделена акцентным цветом
                    Text(
                        prob,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
