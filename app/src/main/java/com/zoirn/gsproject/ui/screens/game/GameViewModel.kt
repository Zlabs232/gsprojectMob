package com.zoirn.gsproject.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//состояние экрана игры-рулетки
data class GameUiState(
    val isLoading: Boolean = true, //первичная загрузка баланса
    val balance: BalanceResponse? = null, //баланс коинов и фриспинов
    val isSpinning: Boolean = false, //идёт ли анимация прокрута
    val spinResult: SpinResultResponse? = null, //результат последнего прокрута
    val showResult: Boolean = false, //показывать ли диалог с результатом
    val error: String? = null, //текст ошибки (null = нет ошибки)

    //история прокрутов
    val showHistory: Boolean = false, //показывать ли диалог истории
    val historyItems: List<SpinHistoryItem> = emptyList(), //список прошлых прокрутов

    //особые призы
    val showPrizes: Boolean = false, //показывать ли диалог призов
    val prizesTab: String = "active", //активная вкладка: "active" или "used"
    val activePrizes: List<SpecialPrizeItem> = emptyList(), //активные призы (можно применить)
    val usedPrizes: List<SpecialPrizeItem> = emptyList(), //использованные призы

    //сообщение о ежедневном фриспине
    val freespinMessage: String? = null,
)

//вьюмодель игрового экрана — управляет рулеткой и призами
class GameViewModel : ViewModel() {

    //внутренний (изменяемый) стейт
    private val _uiState = MutableStateFlow(GameUiState())
    //внешний (только для чтения)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    //при создании сразу загружаем баланс
    init {
        loadBalance()
    }

    //загрузить баланс коинов и фриспинов
    fun loadBalance() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = RetrofitClient.apiService.getGameBalance()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        balance = response.body(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ошибка загрузки баланса"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка сети: ${e.message}"
                )
            }
        }
    }

    //крутануть рулетку (тратит фриспин или 1 коин)
    fun spin() {
        val balance = _uiState.value.balance ?: return //нет баланса — ничего не делаем
        //проверяем что есть ресурсы для прокрута
        if (balance.coins < 1 && balance.freespins < 1) {
            _uiState.value = _uiState.value.copy(error = "Недостаточно средств!")
            return
        }

        viewModelScope.launch {
            //запускаем анимацию прокрута
            _uiState.value = _uiState.value.copy(
                isSpinning = true,
                showResult = false,
                error = null,
                spinResult = null
            )

            try {
                val response = RetrofitClient.apiService.spin() //отправляем запрос на сервер
                if (response.isSuccessful) {
                    val result = response.body()
                    _uiState.value = _uiState.value.copy(spinResult = result) //сохраняем результат

                    //ждём 3 секунды — за это время играет анимация слотов
                    delay(3000)

                    //показываем результат
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        showResult = true,
                        balance = result?.newBalance ?: _uiState.value.balance //обновляем баланс
                    )
                } else {
                    val errorBody = response.errorBody()?.string() //читаем текст ошибки
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        error = errorBody ?: "Ошибка спина"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSpinning = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }

    //закрыть диалог с результатом прокрута
    fun dismissResult() {
        _uiState.value = _uiState.value.copy(showResult = false, spinResult = null)
    }

    //закрыть уведомление об ошибке
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    //получить ежедневный фриспин
    fun claimDailyFreespin() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.claimDailyFreespin()
                if (response.isSuccessful) {
                    val result = response.body()
                    _uiState.value = _uiState.value.copy(
                        balance = result?.balance ?: _uiState.value.balance, //обновляем баланс
                        freespinMessage = result?.message //показываем сообщение
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Фриспин уже получен сегодня"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка: ${e.message}")
            }
        }
    }

    //скрыть сообщение о фриспине
    fun dismissFreespinMessage() {
        _uiState.value = _uiState.value.copy(freespinMessage = null)
    }

    //загрузить и показать историю прокрутов
    fun showHistory() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getSpinHistory()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        showHistory = true,
                        historyItems = response.body()?.history ?: emptyList()
                    )
                }
            } catch (_: Exception) {}
        }
    }

    //скрыть диалог истории
    fun hideHistory() {
        _uiState.value = _uiState.value.copy(showHistory = false)
    }

    //загрузить и показать особые призы
    fun showPrizes() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getSpecialPrizes()
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = _uiState.value.copy(
                        showPrizes = true,
                        activePrizes = body?.active ?: emptyList(), //активные призы
                        usedPrizes = body?.used ?: emptyList() //использованные призы
                    )
                }
            } catch (_: Exception) {}
        }
    }

    //скрыть диалог призов
    fun hidePrizes() {
        _uiState.value = _uiState.value.copy(showPrizes = false)
    }

    //переключить вкладку в диалоге призов (active / used)
    fun setPrizesTab(tab: String) {
        _uiState.value = _uiState.value.copy(prizesTab = tab)
    }

    //продать приз за коины
    fun sellPrize(prizeId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.sellPrize(prizeId)
                if (response.isSuccessful) {
                    loadBalance() //обновляем баланс (добавились коины)
                    showPrizes() //обновляем список призов
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка продажи: ${e.message}")
            }
        }
    }

    //отправить заявку на применение приза (препод должен подтвердить)
    fun requestPrize(prizeId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.requestPrize(prizeId)
                if (response.isSuccessful) {
                    showPrizes() //обновляем список
                    _uiState.value = _uiState.value.copy(freespinMessage = "Заявка отправлена!")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка: ${e.message}")
            }
        }
    }
}
