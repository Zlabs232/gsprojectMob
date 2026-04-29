package com.zoirn.gsproject.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zoirn.gsproject.utils.AppTheme

//цветовая схема для тёмной темы — привязываем наши цвета к слотам Material3
private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent, //основной акцент (кнопки, чипы, иконки)
    onPrimary = DarkOnAccent, //текст поверх primary-цвета
    primaryContainer = DarkAccentVariant, //контейнер для primary элементов
    secondary = DarkAccent, //вторичный акцент (такой же как основной)
    onSecondary = DarkOnAccent,
    background = DarkBackground, //фон всего экрана
    onBackground = DarkText, //текст на фоне
    surface = DarkSurface, //поверхность карточек и диалогов
    onSurface = DarkText, //текст на поверхности
    surfaceVariant = DarkSurfaceVariant, //вариант поверхности (более тёмные карточки)
    onSurfaceVariant = DarkTextSecondary, //вторичный текст на карточках
    error = DarkError, //цвет ошибок
    onError = DarkOnAccent, //текст на ошибке
    outline = DarkOutline //рамки и разделители
)

//цветовая схема для розовой темы — тот же набор но с розовыми цветами
private val PinkColorScheme = darkColorScheme(
    primary = PinkAccent,
    onPrimary = PinkOnAccent,
    primaryContainer = PinkAccentVariant,
    secondary = PinkAccent,
    onSecondary = PinkOnAccent,
    background = PinkBackground,
    onBackground = PinkText,
    surface = PinkSurface,
    onSurface = PinkText,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = PinkTextSecondary,
    error = PinkError,
    onError = PinkOnAccent,
    outline = PinkOutline
)

//главная тема приложения — оборачивает весь UI
@Composable
fun GsProjectTheme(
    appTheme: AppTheme = AppTheme.DARK, //текущая тема (тёмная по умолчанию)
    content: @Composable () -> Unit //содержимое которое будет отрисовано в теме
) {
    //выбираем цветовую схему в зависимости от выбранной темы
    val colorScheme = when (appTheme) {
        AppTheme.DARK -> DarkColorScheme
        AppTheme.PINK -> PinkColorScheme
    }

    val view = LocalView.current
    //SideEffect — выполняется после рендера (безопасно трогать системные API)
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            //красим статус-бар и навигационный бар в цвет фона
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            //указываем что иконки статус-бара и навигации должны быть светлыми
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false //светлые иконки на тёмном фоне
                isAppearanceLightNavigationBars = false
            }
        }
    }

    //применяем тему ко всему содержимому
    MaterialTheme(
        colorScheme = colorScheme, //цвета
        typography = Typography, //шрифты
        content = content //содержимое приложения
    )
}
