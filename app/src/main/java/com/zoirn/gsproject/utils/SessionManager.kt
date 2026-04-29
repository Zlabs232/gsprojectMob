package com.zoirn.gsproject.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

//менеджер сессии — хранит токен и логин в зашифрованном хранилище на телефоне
object SessionManager {

    //название файла с настройками (типа как имя файла на диске)
    private const val PREFS_NAME = "gs_session"
    //ключи для хранения значений (по ним достаём данные)
    private const val KEY_SESSION_TOKEN = "session_token" //ключ токена
    private const val KEY_GITHUB_LOGIN = "github_login" //ключ логина

    //сами зашифрованные настройки (инициализируются в init)
    private lateinit var prefs: SharedPreferences

    //инициализация — создаём зашифрованное хранилище (вызывается один раз в MainActivity)
    fun init(context: Context) {
        //мастер-ключ шифрования на основе AES256-GCM (стандарт шифрования)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        //создаём зашифрованные SharedPreferences (ключи и значения шифруются отдельно)
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME, //имя файла
            masterKey, //ключ шифрования
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, //шифрование ключей
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM //шифрование значений
        )
    }

    //сохранить сессию — записываем токен и логин после успешного входа
    fun saveSession(token: String, githubLogin: String) {
        prefs.edit()
            .putString(KEY_SESSION_TOKEN, token) //сохраняем токен
            .putString(KEY_GITHUB_LOGIN, githubLogin) //сохраняем логин
            .apply() //применяем асинхронно
    }

    //получить токен сессии (null если не авторизован)
    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    //получить github логин пользователя
    fun getGithubLogin(): String? = prefs.getString(KEY_GITHUB_LOGIN, null)

    //очистить сессию — вызывается при выходе из аккаунта
    fun clearSession() {
        prefs.edit().clear().apply() //удаляем все сохранённые данные
    }

    //проверить: авторизован ли пользователь (просто смотрим — есть токен или нет)
    fun isLoggedIn(): Boolean = getSessionToken() != null
}
