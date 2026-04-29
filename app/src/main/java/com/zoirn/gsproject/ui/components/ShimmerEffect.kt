package com.zoirn.gsproject.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

//создаёт анимированную кисть с эффектом шиммера (бегущий блик)
@Composable
fun shimmerBrush(): Brush {
    //три цвета для градиента: тёмный → светлый → тёмный
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), //центр — ярче
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )
    //бесконечная анимация для шиммера
    val transition = rememberInfiniteTransition(label = "shimmer")
    //анимирую позицию градиента от 0 до 1000 пикселей (блик движется вправо)
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing), //1.2 секунды, линейно
            repeatMode = RepeatMode.Restart //начинаю сначала после каждого прохода
        ),
        label = "shimmer_translate"
    )
    //возвращаем линейный градиент с движущимся положением
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, 0f), //начало чуть левее
        end = Offset(translateAnim.value, 0f) //конец — текущая позиция
    )
}

//заглушка-скелет для списков (пока грузятся данные)
@Composable
fun ShimmerListPlaceholder(itemCount: Int = 5) {
    val brush = shimmerBrush() //беру анимированную кисть
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //шиммер-заголовок — половина ширины экрана
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        Spacer(Modifier.height(4.dp))
        //повторяю карточку-заглушку нужное колво раз
        repeat(itemCount) {
            ShimmerCard(brush)
        }
    }
}

//одна карточка-заглушка
@Composable
private fun ShimmerCard(brush: Brush) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp) //высота, как у обычной карточки
            .clip(RoundedCornerShape(12.dp))
            .background(brush) //заполняю анимированным шиммером
    )
}

//заглушка для главного экрана — повторяет структуру (чипы, поиск, карточки реп, кнопки)
@Composable
fun ShimmerMainPlaceholder() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        //заглушка для чипов выбора предмета
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(70.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(brush))
            Box(Modifier.width(60.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(brush))
        }
        //заглушка строки поиска репозитория
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        //заглушки карточек репозиториев (4 штуки)
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        }
        Spacer(Modifier.height(8.dp))
        //заглушки сетки кнопок действий
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(14.dp)).background(brush))
            Box(Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(14.dp)).background(brush))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(14.dp)).background(brush))
            Box(Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(14.dp)).background(brush))
        }
    }
}

//заглушка для экрана профиля/баллов — аватарка, имя, карточки статистики
@Composable
fun ShimmerProfilePlaceholder() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        //заглушка шапки профиля — круглая аватарка + имя и группа
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(brush)) //круглая аватарка
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush)) //имя
                Box(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush)) //группа
            }
        }
        //заглушка карточки со статистикой
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(brush))
        //заглушки строк
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(10.dp)).background(brush))
        }
    }
}
