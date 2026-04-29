package com.zoirn.gsproject.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

//типографика — настройки шрифтов для всего приложения
val Typography = Typography(
    //стиль для основного текста (параграфы, описания)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, //системный шрифт
        fontWeight = FontWeight.Normal, //обычный (не жирный)
        fontSize = 16.sp, //размер 16sp
        lineHeight = 24.sp, //высота строки 24sp
        letterSpacing = 0.5.sp //межбуквенный интервал
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
