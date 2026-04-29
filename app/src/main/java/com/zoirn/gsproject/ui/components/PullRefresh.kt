package com.zoirn.gsproject.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

//компонент с поддержкой "потяни чтобы обновить"
@Composable
fun PullRefreshLayout(
    isRefreshing: Boolean, //флаг: идёт ли сейчас загрузка
    onRefresh: () -> Unit, //колбэк — вызывается когда юзер потянул вниз
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit //содержимое, которое можно будет обновить
) {
    //порог в пикселях — насколько нужно потянуть чтобы сработало обновление (80dp)
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }
    //текущее смещение пальца вниз (в пикселях)
    var pullOffset by remember { mutableFloatStateOf(0f) }
    //флаг: уже сработал триггер обновления или нет
    var triggered by remember { mutableStateOf(false) }

    //когда загрузка завершилась — плавно возвращаем индикатор обратно
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            animate(pullOffset, 0f) { value, _ -> pullOffset = value } //анимация возврата
            triggered = false
        }
    }

    //обработчик вложенной прокрутки — перехватывает жесты до и после скролла
    val connection = remember {
        object : NestedScrollConnection {
            //вызывается ДО того как прокрутка передаётся дочерним элементам
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                //если пользователь скролит вверх и индикатор выдвинут — убираем его
                if (source == NestedScrollSource.UserInput && pullOffset > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-pullOffset) //не убираем больше чем есть
                    pullOffset += consumed
                    return Offset(0f, consumed) //говорим что потребили это смещение
                }
                return Offset.Zero
            }

            //вызывается ПОСЛЕ того как дочерние элементы обработали прокрутку
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                //если список докрутили до верха и тянем дальше вниз — двигаем индикатор
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pullOffset = (pullOffset + available.y * 0.5f).coerceAtMost(threshold * 2f) //с сопротивлением 0.5
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            //вызывается после того как пользователь отпустил палец (fling = бросок)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullOffset >= threshold && !triggered) {
                    triggered = true
                    onRefresh() //достаточно потянули — запускаем обновление
                } else if (!triggered) {
                    animate(pullOffset, 0f) { value, _ -> pullOffset = value } //не хватило — убираем
                }
                return Velocity.Zero
            }
        }
    }

    //Box с подключённым обработчиком прокрутки
    Box(modifier.nestedScroll(connection)) {
        content() //содержимое экрана
        //показываем кружок загрузки если потянули или идёт обновление
        if (pullOffset > 10f || isRefreshing) {
            Box(
                Modifier
                    .fillMaxWidth()
                    //позиционируем кружок в зависимости от того насколько потянули
                    .offset(y = with(LocalDensity.current) { (pullOffset.coerceAtMost(threshold) - 40.dp.toPx()).toDp() })
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                //сам крутящийся кружок индикатора загрузки
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
