package com.zoirn.gsproject.ui.screens.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.viewinterop.AndroidView
import com.zoirn.gsproject.BuildConfig
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.utils.SessionManager
import kotlinx.coroutines.launch

//главный экран входа — проверяет сессию и показывает нужный контент
@Composable
fun LoginScreen(
    onLoginSuccess: (needsProfile: Boolean) -> Unit //колбэк после успешного входа
) {
    var showWebView by remember { mutableStateOf(false) } //показывать ли вебвью с гитхабом
    var isLoading by remember { mutableStateOf(true) } //идёт ли проверка сохранённой сессии
    var errorMessage by remember { mutableStateOf<String?>(null) } //текст ошибки (если есть)
    val scope = rememberCoroutineScope() //корутина для асинхронных операций
    val context = LocalContext.current //контекст андроида (нужен для SessionManager)

    //при запуске проверяем — есть ли сохранённая сессия (был ли юзер раньше)
    LaunchedEffect(Unit) {
        if (SessionManager.isLoggedIn()) {
            RetrofitClient.setSessionToken(SessionManager.getSessionToken()) //подставляем токен в ретрофит
            try {
                val response = RetrofitClient.apiService.getCurrentUser() //пробуем получить данные юзера
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        //проверяем заполнен ли профиль (есть имя и фамилия)
                        val needsProfile = user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank()
                        onLoginSuccess(needsProfile) //сразу переходим дальше
                        return@LaunchedEffect
                    }
                }
                //сессия устарела — чистим
                SessionManager.clearSession()
                RetrofitClient.setSessionToken(null)
            } catch (e: Exception) {
                //нет сети — чистим сессию, остаёмся на логине
                SessionManager.clearSession()
                RetrofitClient.setSessionToken(null)
            }
        }
        isLoading = false //проверка закончена — показываем экран
    }

    //пока проверяем сессию — показываем крутилку по центру
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    //если нажали "войти" — показываем вебвью с github oauth
    if (showWebView) {
        OAuthWebView(
            onSessionObtained = { token, needsProfile ->
                SessionManager.saveSession(token, "") //временно сохраняем токен (логин ещё не знаем)
                RetrofitClient.setSessionToken(token)
                //получаем реальные данные юзера чтобы сохранить github_login
                scope.launch {
                    try {
                        val response = RetrofitClient.apiService.getCurrentUser()
                        if (response.isSuccessful) {
                            val user = response.body()
                            if (user != null) {
                                SessionManager.saveSession(token, user.githubLogin) //теперь сохраняем с логином
                                val profileNeeded = user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank()
                                onLoginSuccess(profileNeeded)
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Ошибка получения данных: ${e.message}"
                        showWebView = false //закрываем вебвью при ошибке
                    }
                }
            },
            onError = { error ->
                errorMessage = error //показываем ошибку
                showWebView = false
            },
            onCancel = {
                showWebView = false //пользователь нажал назад
            }
        )
    } else {
        //стандартный экран с кнопкой "войти через github"
        LoginContent(
            errorMessage = errorMessage,
            onLoginClick = {
                errorMessage = null
                showWebView = true //открываем вебвью
            }
        )
    }
}

//UI экрана входа — анимированный фон + карточка с кнопкой
@Composable
private fun LoginContent(
    errorMessage: String?, //текст ошибки (null если нет ошибки)
    onLoginClick: () -> Unit //нажатие кнопки "Авторизоваться"
) {
    //бесконечная анимация для фоновых кружков
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    //анимация первого кружка — вращается за 20 секунд
    val circleOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circle1"
    )

    //анимация второго кружка — вращается в обратную сторону за 15 секунд
    val circleOffset2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circle2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        //анимированные размытые кружки на фоне (декоративные)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp) //сильное размытие — эффект свечения
        ) {
            val w = size.width
            val h = size.height

            //синеватый кружок — двигается по синусоиде в левой части
            drawCircle(
                color = Color(0xFF667EEA).copy(alpha = 0.15f),
                radius = 250f,
                center = Offset(
                    x = w * 0.2f + kotlin.math.sin(Math.toRadians(circleOffset1.toDouble())).toFloat() * 50f,
                    y = h * 0.3f + kotlin.math.cos(Math.toRadians(circleOffset1.toDouble())).toFloat() * 50f
                )
            )

            //фиолетовый кружок — двигается в правой нижней части
            drawCircle(
                color = Color(0xFF764BA2).copy(alpha = 0.15f),
                radius = 200f,
                center = Offset(
                    x = w * 0.8f + kotlin.math.sin(Math.toRadians(circleOffset2.toDouble())).toFloat() * 40f,
                    y = h * 0.7f + kotlin.math.cos(Math.toRadians(circleOffset2.toDouble())).toFloat() * 40f
                )
            )

            //розово-красный кружок — в правой верхней части
            drawCircle(
                color = Color(0xFFF5576C).copy(alpha = 0.1f),
                radius = 150f,
                center = Offset(
                    x = w * 0.6f + kotlin.math.sin(Math.toRadians(circleOffset1.toDouble() * 0.7)).toFloat() * 30f,
                    y = h * 0.2f + kotlin.math.cos(Math.toRadians(circleOffset2.toDouble() * 0.7)).toFloat() * 30f
                )
            )
        }

        //основной контент поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            //название приложения
            Text(
                text = "gsProject",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            //подзаголовок
            Text(
                text = "Студенческий портал",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            //карточка с кнопкой входа
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) //полупрозрачный фон
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Студент",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Вход через GitHub",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    //кнопка с градиентным фоном (Material3 не поддерживает градиент нативно — делаем через Box)
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent //фон прозрачный — используем Box с градиентом
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF667EEA), //синий
                                            Color(0xFF764BA2) //фиолетовый
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Авторизоваться",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            //карточка с ошибкой — показывается если что-то пошло не так
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

//вебвью для OAuth авторизации через GitHub
@SuppressLint("SetJavaScriptEnabled") //разрешаем JS (нужен для GitHub OAuth)
@Composable
private fun OAuthWebView(
    onSessionObtained: (token: String, needsProfile: Boolean) -> Unit, //токен получен успешно
    onError: (String) -> Unit, //что-то пошло не так
    onCancel: () -> Unit //пользователь нажал назад
) {
    var isLoading by remember { mutableStateOf(true) } //загружается ли страница

    Box(modifier = Modifier.fillMaxSize()) {
        //встраиваем нативный WebView в Compose через AndroidView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true //включаем JS (без него гитхаб не работает)
                    settings.domStorageEnabled = true //включаем хранилище для JS

                    //чистим старые куки перед новым входом
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    //клиент для отслеживания событий вебвью
                    webViewClient = object : WebViewClient() {
                        //страница начала загружаться
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        //страница загрузилась — проверяем куда нас перенаправили
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false

                            url ?: return

                            //если попали на mainstudent.html или profile.html — авторизация прошла успешно
                            if (url.contains("mainstudent.html") || url.contains("profile.html")) {
                                //извлекаем session_token из куки
                                val baseUrl = BuildConfig.BASE_URL
                                val cookies = CookieManager.getInstance().getCookie(baseUrl)

                                //парсим строку куки и ищем session_token
                                val sessionToken = cookies?.split(";")
                                    ?.map { it.trim() }
                                    ?.find { it.startsWith("session_token=") }
                                    ?.substringAfter("session_token=")

                                if (sessionToken != null) {
                                    val needsProfile = url.contains("profile.html") //если profile.html — профиль не заполнен
                                    onSessionObtained(sessionToken, needsProfile)
                                } else {
                                    onError("Не удалось получить токен сессии")
                                }
                            }
                        }

                        //решаем — открыть ссылку внутри вебвью или во внешнем браузере
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false

                            //github и наш сервер — держим внутри вебвью
                            if (url.contains("github.com") ||
                                url.contains(BuildConfig.BASE_URL) ||
                                url.contains("lmsgsproject.ru")
                            ) {
                                return false //false = открыть в вебвью
                            }

                            return false
                        }
                    }

                    //загружаем страницу входа через GitHub
                    loadUrl("${BuildConfig.API_BASE_URL}auth/github/login")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        //пока страница грузится — полупрозрачная заглушка с кружком
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        //кнопка "назад" в левом верхнем углу
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
