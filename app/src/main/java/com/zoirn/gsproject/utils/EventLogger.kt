package com.zoirn.gsproject.utils

import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.StudentEventRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//логгер событий — отправляет на сервер что студент делал в приложении (для аналитики)
object EventLogger {
    //корутина на IO-потоке (не блокирует основной поток интерфейса)
    private val scope = CoroutineScope(Dispatchers.IO)

    //залогировать событие: action = что сделал, label = уточнение (какой экран и т.д.)
    fun log(action: String, label: String? = null) {
        val login = SessionManager.getGithubLogin() ?: return //если не авторизован — ничего не логируем
        scope.launch {
            try {
                //отправляем событие на сервер в фоне
                RetrofitClient.apiService.logEvent(
                    StudentEventRequest(githubLogin = login, action = action, label = label)
                )
            } catch (_: Exception) {} //если ошибка — просто игнорируем (аналитика не критична)
        }
    }
}
