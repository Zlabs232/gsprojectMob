package com.zoirn.gsproject.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.ProfileRequest
import com.zoirn.gsproject.utils.SessionManager
import kotlinx.coroutines.launch

//список доступных учебных групп
private val GROUPS = listOf("ИСП-231", "ИСП-232", "ИСП-233")

//экран заполнения профиля — вводим ФИО, никнейм и группу
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileSaved: () -> Unit //колбэк после успешного сохранения
) {
    //поля формы — каждое поле — отдельный стейт
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") } //выбранная группа
    var groupExpanded by remember { mutableStateOf(false) } //открыт ли дропдаун групп
    var isLoading by remember { mutableStateOf(false) } //идёт ли сохранение
    var isFetchingProfile by remember { mutableStateOf(true) } //загружается ли текущий профиль
    var errorMessage by remember { mutableStateOf<String?>(null) } //ошибка валидации или сети

    val scope = rememberCoroutineScope()

    //при открытии экрана — пробуем загрузить уже заполненный профиль (если был)
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getStudentProfile()
            if (response.isSuccessful) {
                val profile = response.body()
                if (profile != null) {
                    //подставляем существующие данные в поля
                    lastName = profile.lastName ?: ""
                    firstName = profile.firstName ?: ""
                    middleName = profile.middleName ?: ""
                    nickname = profile.nickname ?: ""
                    selectedGroup = profile.groupName ?: ""
                }
            }
        } catch (_: Exception) {
            //профиля ещё нет — ничего страшного, просто пустые поля
        }
        isFetchingProfile = false //загрузка завершена
    }

    //анимация для фоновых кружков
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val anim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)), //18 секунд оборот
        label = "anim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        //декоративные фоновые кружки с размытием
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            drawCircle(
                color = Color(0xFF667EEA).copy(alpha = 0.12f),
                radius = 220f,
                center = Offset(
                    size.width * 0.3f + kotlin.math.sin(Math.toRadians(anim.toDouble())).toFloat() * 40f,
                    size.height * 0.25f
                )
            )
            drawCircle(
                color = Color(0xFF764BA2).copy(alpha = 0.12f),
                radius = 180f,
                center = Offset(
                    size.width * 0.75f,
                    size.height * 0.7f + kotlin.math.cos(Math.toRadians(anim.toDouble())).toFloat() * 30f
                )
            )
        }

        //пока загружаем профиль — показываем крутилку
        if (isFetchingProfile) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Box //прерываем отрисовку остального
        }

        //скролл-контент (форма может не влезть на экран)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            //заголовок экрана
            Text(
                text = "Кастомизация профиля",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            //карточка с полями формы
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //поле фамилии (обязательное)
                    ProfileTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        placeholder = "Фамилия *",
                        imeAction = ImeAction.Next //клавиша "далее" переводит на следующее поле
                    )

                    //поле имени (обязательное)
                    ProfileTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = "Имя *",
                        imeAction = ImeAction.Next
                    )

                    //поле отчества (необязательное)
                    ProfileTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        placeholder = "Отчество",
                        imeAction = ImeAction.Next
                    )

                    //поле никнейма (необязательное)
                    ProfileTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        placeholder = "Никнейм",
                        imeAction = ImeAction.Done
                    )

                    //выпадающий список для выбора учебной группы
                    ExposedDropdownMenuBox(
                        expanded = groupExpanded,
                        onExpandedChange = { groupExpanded = it } //открыть/закрыть при нажатии
                    ) {
                        //текстовое поле которое нельзя редактировать вручную
                        OutlinedTextField(
                            value = selectedGroup.ifEmpty { "" },
                            onValueChange = {},
                            readOnly = true, //только чтение — выбор через дропдаун
                            placeholder = {
                                Text(
                                    "Выберите группу *",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                //иконка стрелки справа
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable), //привязка к дропдауну
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        //выпадающий список групп
                        ExposedDropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false }
                        ) {
                            //пункт меню для каждой группы
                            GROUPS.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group) },
                                    onClick = {
                                        selectedGroup = group //сохраняем выбор
                                        groupExpanded = false //закрываем список
                                    }
                                )
                            }
                        }
                    }

                    //подсказка под дропдауном
                    Text(
                        text = "Выберите вашу учебную группу",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    //карточка с ошибкой (показывается если не прошла валидация)
                    if (errorMessage != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }

                    //кнопка сохранить
                    Button(
                        onClick = {
                            //валидация полей перед отправкой
                            when {
                                lastName.isBlank() -> {
                                    errorMessage = "Введите фамилию"
                                    return@Button
                                }
                                firstName.isBlank() -> {
                                    errorMessage = "Введите имя"
                                    return@Button
                                }
                                selectedGroup.isBlank() -> {
                                    errorMessage = "Выберите группу"
                                    return@Button
                                }
                                //проверка на XSS (угловые скобки)
                                lastName.contains("<") || lastName.contains(">") ||
                                firstName.contains("<") || firstName.contains(">") -> {
                                    errorMessage = "Недопустимые символы в полях"
                                    return@Button
                                }
                            }

                            errorMessage = null
                            isLoading = true

                            //отправляем профиль на сервер
                            scope.launch {
                                try {
                                    val githubLogin = SessionManager.getGithubLogin() ?: ""
                                    //формируем объект запроса
                                    val profile = ProfileRequest(
                                        githubLogin = githubLogin,
                                        firstName = firstName.trim(), //убираем пробелы по краям
                                        lastName = lastName.trim(),
                                        middleName = middleName.trim().ifEmpty { null }, //пустое = null
                                        nickname = nickname.trim().ifEmpty { null },
                                        groupName = selectedGroup
                                    )

                                    val response = RetrofitClient.apiService.saveProfile(profile)
                                    if (response.isSuccessful) {
                                        onProfileSaved() //переходим на главный экран
                                    } else {
                                        errorMessage = "Ошибка сохранения: ${response.code()}"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка сети: ${e.message}"
                                } finally {
                                    isLoading = false //в любом случае убираем крутилку
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading, //блокируем кнопку пока идёт сохранение
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        //в зависимости от состояния показываем крутилку или текст
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Сохранить профиль",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

//переиспользуемое текстовое поле для формы профиля
@Composable
private fun ProfileTextField(
    value: String, //текущее значение
    onValueChange: (String) -> Unit, //колбэк при изменении
    placeholder: String, //подсказка в пустом поле
    imeAction: ImeAction //что показывать на клавиатуре: "далее" или "готово"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true, //одна строка
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words, //автоматически с большой буквы
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
