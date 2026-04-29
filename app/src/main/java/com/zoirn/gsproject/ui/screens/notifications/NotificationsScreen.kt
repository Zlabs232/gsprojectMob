package com.zoirn.gsproject.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.MarkReadRequest
import com.zoirn.gsproject.data.model.NotificationItem
import com.zoirn.gsproject.ui.components.PullRefreshLayout
import com.zoirn.gsproject.ui.components.ShimmerListPlaceholder
import com.zoirn.gsproject.utils.EventLogger
import kotlinx.coroutines.launch

//экран уведомлений от преподавателя
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) } //первичная загрузка
    var isRefreshing by remember { mutableStateOf(false) } //pull-to-refresh
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) } //список уведомлений
    var error by remember { mutableStateOf<String?>(null) } //ошибка
    val scope = rememberCoroutineScope()

    //загрузка уведомлений
    fun loadData() {
        scope.launch {
            error = null
            try {
                val resp = RetrofitClient.apiService.getNotifications()
                if (resp.isSuccessful) {
                    notifications = resp.body()?.notifications ?: emptyList()
                }
            } catch (e: Exception) {
                error = "Ошибка загрузки: ${e.message}"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    //логируем открытие и сразу загружаем
    LaunchedEffect(Unit) {
        EventLogger.log("screen_view", "notifications")
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомления", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        //разные состояния экрана
        if (isLoading) {
            //заглушка пока грузится
            Box(Modifier.fillMaxSize().padding(padding)) {
                ShimmerListPlaceholder()
            }
        } else if (error != null && notifications.isEmpty()) {
            //ошибка и нет данных — показываем ошибку по центру
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    Button(onClick = { isLoading = true; loadData() }) {
                        Text("Повторить")
                    }
                }
            }
        } else if (notifications.isEmpty()) {
            //нет уведомлений — пустой экран с текстом
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет уведомлений", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
        } else {
            //есть уведомления — список с pull-to-refresh
            PullRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; loadData() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                //каждое уведомление — отдельная карточка
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(
                        notif = notif,
                        onMarkRead = {
                            //при нажатии на непрочитанное — помечаем прочитанным
                            scope.launch {
                                try {
                                    RetrofitClient.apiService.markNotificationRead(MarkReadRequest(notif.id))
                                    //обновляем локальный список без перезагрузки с сервера
                                    notifications = notifications.map {
                                        if (it.id == notif.id) it.copy(isRead = 1) else it
                                    }
                                } catch (_: Exception) {} //ошибка не критична
                            }
                        }
                    )
                }
            }
            }
        }
    }
}

//карточка одного уведомления
@Composable
private fun NotificationCard(notif: NotificationItem, onMarkRead: () -> Unit) {
    val isUnread = notif.isRead == 0 //непрочитанное если isRead = 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            //непрочитанные кликабельны — нажатие помечает их прочитанными
            .then(if (isUnread) Modifier.clickable { onMarkRead() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            //непрочитанные — с лёгкой акцентной подсветкой
            containerColor = if (isUnread)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            //маленькая цветная точка слева — индикатор непрочитанного
            if (isUnread) {
                Icon(
                    Icons.Default.Circle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp).padding(top = 6.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                //заголовок — жирный если непрочитанное
                Text(
                    notif.title,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                //текст уведомления
                Text(notif.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                //дата (берём первые 16 символов — дата + время без секунд, T заменяем на пробел)
                Text(
                    notif.createdAt.take(16).replace("T", " "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
