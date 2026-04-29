package com.zoirn.gsproject.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//состояние главного экрана — всё что отображается хранится здесь
data class MainUiState(
    val isLoading: Boolean = true, //первичная загрузка (показывает шиммер)
    val isRefreshing: Boolean = false, //обновление через pull-to-refresh
    val profile: ProfileResponse? = null, //данные профиля студента
    val repos: List<RepoItem> = emptyList(), //отфильтрованные репозитории
    val balance: BalanceResponse? = null, //баланс коинов и фриспинов
    val unreadNotifications: Int = 0, //количество непрочитанных уведомлений (для бейджа)
    val error: String? = null, //текст ошибки (null = нет ошибки)

    //фильтры для списка репозиториев
    val selectedSubject: String = "ISRPO", //выбранный предмет
    val selectedRmpSub: String = "Kotlin", //подкатегория РМП
    val repoSearchQuery: String = "", //текст поиска в репозиториях
)

//вьюмодель главного экрана — загружает данные и управляет состоянием
class MainViewModel : ViewModel() {

    //внутренний стейт (mutable — только ViewModel может менять)
    private val _uiState = MutableStateFlow(MainUiState())
    //внешний стейт (readonly — экран только читает)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    //все репозитории до фильтрации (нужно хранить отдельно чтобы фильтры работали)
    private var allRepos: List<RepoItem> = emptyList()

    //при создании вьюмодели сразу загружаем все данные
    init {
        loadAll()
    }

    //загрузить все данные параллельно (профиль, репы, баланс, уведомления)
    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                //запускаем все 4 загрузки одновременно в отдельных корутинах
                val profileJob = launch { loadProfile() }
                val reposJob = launch { loadRepos() }
                val balanceJob = launch { loadBalance() }
                val notifJob = launch { loadNotifications() }

                //ждём пока все завершатся
                profileJob.join()
                reposJob.join()
                balanceJob.join()
                notifJob.join()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    //обновить данные (для pull-to-refresh — не показывает шиммер, только крутилку сверху)
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                //параллельное обновление всех данных
                val profileJob = launch { loadProfile() }
                val reposJob = launch { loadRepos() }
                val balanceJob = launch { loadBalance() }
                val notifJob = launch { loadNotifications() }

                profileJob.join()
                reposJob.join()
                balanceJob.join()
                notifJob.join()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка: ${e.message}")
            }
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    //загрузить профиль студента
    private suspend fun loadProfile() {
        try {
            val response = RetrofitClient.apiService.getStudentProfile()
            if (response.isSuccessful) {
                _uiState.value = _uiState.value.copy(profile = response.body())
            }
        } catch (_: Exception) {} //ошибка не критична — показываем что есть
    }

    //загрузить репозитории
    private suspend fun loadRepos() {
        try {
            val response = RetrofitClient.apiService.getStudentRepos()
            if (response.isSuccessful) {
                allRepos = response.body()?.repos ?: emptyList()
                applyFilters() //применяем текущие фильтры к новым данным
            }
        } catch (_: Exception) {}
    }

    //загрузить баланс коинов и фриспинов
    private suspend fun loadBalance() {
        try {
            val response = RetrofitClient.apiService.getGameBalance()
            if (response.isSuccessful) {
                _uiState.value = _uiState.value.copy(balance = response.body())
            }
        } catch (_: Exception) {}
    }

    //загрузить уведомления и посчитать непрочитанные
    private suspend fun loadNotifications() {
        try {
            val response = RetrofitClient.apiService.getNotifications()
            if (response.isSuccessful) {
                val notifs = response.body()?.notifications ?: emptyList()
                val unread = notifs.count { it.isRead == 0 } //считаем непрочитанные
                _uiState.value = _uiState.value.copy(unreadNotifications = unread)
            }
        } catch (_: Exception) {}
    }

    //сменить выбранный предмет (ИСРПО / РМП)
    fun setSubject(subject: String) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        applyFilters() //перефильтровать репозитории
    }

    //сменить подкатегорию РМП (Kotlin / AnёdroidStudio)
    fun setRmpSub(sub: String) {
        _uiState.value = _uiState.value.copy(selectedRmpSub = sub)
        applyFilters()
    }

    //изменить текст поиска репозиториев
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(repoSearchQuery = query)
        applyFilters()
    }

    //применить фильтры — оставляем только репы подходящие по предмету и поиску
    private fun applyFilters() {АЛО
        val state = _uiState.value
        var filtered = allRepos

        //фильтр по предмету
        filtered = when (state.selectedSubject) {
            "ISRPO" -> filtered.filter {
                it.name.lowercase().contains("isrpo") //оставляем только репы с isrpo в названии
            }
            "RMP" -> {
                val sub = state.selectedRmpSub.lowercase()
                filtered.filter { repo ->
                    val name = repo.name.lowercase()
                    when (sub) {
                        "kotlin" -> name.contains("kotlin") //только kotlin-репы
                        "androidstudio" -> name.contains("android") || //android/compose/mobile
                                name.contains("compose") ||
                                name.contains("mobile")
                        else -> name.contains("kotlin") || name.contains("android") //всё РМП
                    }
                }
            }
            else -> filtered //без фильтра
        }

        //фильтр по тексту поиска
        if (state.repoSearchQuery.isNotBlank()) {
            val query = state.repoSearchQuery.lowercase()
            filtered = filtered.filter { it.name.lowercase().contains(query) }
        }

        _uiState.value = _uiState.value.copy(repos = filtered) //обновляем список в стейте
    }

    //выход из аккаунта — чистим сессию
    fun logout() {
        com.zoirn.gsproject.utils.SessionManager.clearSession() //удаляем токен из зашифрованного хранилища
        RetrofitClient.setSessionToken(null) //удаляем токен из ретрофита
    }
}
