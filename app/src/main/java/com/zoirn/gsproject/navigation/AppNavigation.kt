package com.zoirn.gsproject.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zoirn.gsproject.ui.screens.game.GameScreen
import com.zoirn.gsproject.ui.screens.homework.HomeworkScreen
import com.zoirn.gsproject.ui.screens.login.LoginScreen
import com.zoirn.gsproject.ui.screens.main.MainStudentScreen
import com.zoirn.gsproject.ui.screens.main.WeeklyTasksScreen
import com.zoirn.gsproject.ui.screens.notifications.NotificationsScreen
import com.zoirn.gsproject.ui.screens.profile.ProfileSetupScreen
import com.zoirn.gsproject.ui.screens.scores.ScoresScreen
import com.zoirn.gsproject.ui.screens.scores.SearchStudentScreen

//объект с маршрутами — строковые константы для навигации между экранами
object Routes {
    const val LOGIN = "login" //экран входа
    const val PROFILE = "profile" //экран настройки профиля
    const val MAIN = "main" //главный экран студента
    const val GAME = "game" //экран игры-рулетки
    const val SCORES = "scores" //экран с баллами
    const val SCORES_PERIOD = "scores_period" //баллы за период - это заглушка, надо удалить))
    const val SEARCH_STUDENT = "search_student" //поиск студента
    const val WEEKLY_TASKS = "weekly_tasks" //задания на неделю
    const val HOMEWORK = "homework" //домашки
    const val NOTIFICATIONS = "notifications" //уведомления
}

//длительность анимаций переходов между экранами в ms
private const val ANIM_DURATION = 300

//главный граф навигации — описывает все экраны и переходы между ними
@Composable
fun AppNavGraph(
    navController: NavHostController, //контроллер навигации (управляет стеком экранов)
    startDestination: String //начальный экран (login или main, зависит от сессии)
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        //анимация входа нового экрана — едет слева направо + появление
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION)) +
                    fadeIn(tween(ANIM_DURATION))
        },
        //анимация выхода текущего экрана — уезжает влево + исчезновение
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION)) +
                    fadeOut(tween(ANIM_DURATION))
        },
        //анимация возврата назад — едет справа налево
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) +
                    fadeIn(tween(ANIM_DURATION))
        },
        //анимация при нажатии назад — уезжает вправо
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) +
                    fadeOut(tween(ANIM_DURATION))
        }
    ) {
        //экран авторизации — вход через гит
        composable(
            Routes.LOGIN,
            enterTransition = { fadeIn(tween(400)) }, //просто плавное появление
            exitTransition = { fadeOut(tween(400)) }
        ) {
            LoginScreen(
                onLoginSuccess = { needsProfile ->
                    //после логина: если нет профиля — идём заполнять, иначе — на главный
                    val destination = if (needsProfile) Routes.PROFILE else Routes.MAIN
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true } //убираем логин из стека (назад не вернуться)
                    }
                }
            )
        }

        //экран настройки профиля — имя, фамилия, группа
        composable(
            Routes.PROFILE,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            ProfileSetupScreen(
                onProfileSaved = {
                    //после сохранения профиля идём на глав экран
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PROFILE) { inclusive = true } //убираем профиль из стека
                    }
                }
            )
        }

        //главный экран — список реп, кнопки, баланс коинов
        composable(
            Routes.MAIN,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = {
                //при переходе вперёд — уезжаем влево
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION)) +
                        fadeOut(tween(ANIM_DURATION))
            },
            popEnterTransition = {
                //при возврате назад — въезжаем справа
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) +
                        fadeIn(tween(ANIM_DURATION))
            }
        ) {
            MainStudentScreen(
                onNavigateToGame = { navController.navigate(Routes.GAME) }, //кнопка "Игра"
                onNavigateToScores = { navController.navigate(Routes.SCORES) }, //кнопка "Баллы"
                onNavigateToSearch = { navController.navigate(Routes.SEARCH_STUDENT) }, //кнопка "Поиск"
                onNavigateToWeeklyTasks = { navController.navigate(Routes.WEEKLY_TASKS) }, //кнопка "Задания"
                onNavigateToHomework = { navController.navigate(Routes.HOMEWORK) }, //кнопка "Домашки"
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) }, //кнопка уведомлений
                onLogout = {
                    //при выходе из аккаунта — очищаем весь стек и идём на логин
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        //экран бурмалды
        composable(Routes.GAME) {
            GameScreen(onBack = { navController.popBackStack() })
        }

        //экран с баллами
        composable(Routes.SCORES) {
            ScoresScreen(onBack = { navController.popBackStack() })
        }

        //заглушка для экрана баллов за период (ещё не сделан)
        composable(Routes.SCORES_PERIOD) {
            PlaceholderScreen("Баллы за период")
        }

        //экран поиска студентов
        composable(Routes.SEARCH_STUDENT) {
            SearchStudentScreen(onBack = { navController.popBackStack() })
        }

        //экран заданий на неделю
        composable(Routes.WEEKLY_TASKS) {
            WeeklyTasksScreen(onBack = { navController.popBackStack() })
        }

        //экран домашек
        composable(Routes.HOMEWORK) {
            HomeworkScreen(onBack = { navController.popBackStack() })
        }

        //экран уведомлений
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
    }
}

//заглушка-экран для неготовых разделов — просто показывает название
@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
