package com.zoirn.gsproject.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//расширение контекста — создаём DataStore для хранения настроек приложения
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

//перечисление доступных тем
enum class AppTheme {
    DARK, //тёмная тема (фиолетовые акценты)
    PINK //розовая тема
}

//менеджер настроек темы — читает и сохраняет выбранную тему
object ThemePreferences {
    //ключ под которым хранится тема в DataStore
    private val THEME_KEY = stringPreferencesKey("app_theme")

    //получить текущую тему как поток данных (Flow — обновляется автоматически при изменении)
    fun getTheme(context: Context): Flow<AppTheme> {
        return context.dataStore.data.map { preferences ->
            //читаем строку из настроек и конвертируем в enum
            when (preferences[THEME_KEY]) {
                "PINK" -> AppTheme.PINK //если сохранено "PINK" — розовая тема
                else -> AppTheme.DARK //всё остальное (или null) — тёмная тема
            }
        }
    }

    //сохранить выбранную тему (suspend — вызывается из корутины)
    suspend fun setTheme(context: Context, theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name //сохраняем название enum как строку
        }
    }
}
