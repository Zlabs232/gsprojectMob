package com.zoirn.gsproject.data.model

import com.google.gson.annotations.SerializedName

//ответ от /game/balance — баланс игровых ресов
data class BalanceResponse(
    val coins: Double, //количество коинов
    val freespins: Int, //количество фриспинов
    @SerializedName("streak_days") val streakDays: Int? = null, //сколько дней подряд заходит в приложение
    @SerializedName("streak_bonus") val streakBonus: Double? = null, //бонусный множитель за стрик
    @SerializedName("daily_freespin_available") val dailyFreespinAvailable: Boolean? = null //доступен ли ежедневный фриспин
)

//ответ после получения ежедневного фриспина
data class FreespinResponse(
    val granted: Boolean, //выдали фриспин или нет
    @SerializedName("streak_days") val streakDays: Int, //текущий стрик в днях
    @SerializedName("freespins_awarded") val freespinsAwarded: Int, //сколько фриспинов дали
    val message: String, //сообщение от сервера
    val balance: BalanceResponse //новый баланс после получения
)

//ответ от /game/weekly-tasks — задания на текущую неделю
data class WeeklyTasksResponse(
    @SerializedName("github_login") val githubLogin: String, //чьи задания
    @SerializedName("week_start") val weekStart: String, //начало недели
    val tasks: List<WeeklyTaskItem> //список заданий
)

//одно задание на неделю
data class WeeklyTaskItem(
    val id: String? = null,
    val type: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val completed: Boolean,
    val reward: Double,
    @SerializedName("reward_claimed") val rewardClaimed: Boolean? = null //получена ли награда
)

//ответ после проверки заданий
data class CheckTasksResponse(
    val tasks: List<WeeklyTaskItem>? = null, //обновлённый список заданий
    val message: String? = null, //сообщение
    @SerializedName("coins_earned") val coinsEarned: Double? = null //сколько коинов заработал
)

//ответ после прокрута рулетки
data class SpinResultResponse(
    val prize: PrizeItem? = null, //выпавший приз
    @SerializedName("new_balance") val newBalance: BalanceResponse? = null, //новый баланс после прокрута
    @SerializedName("used_freespin") val usedFreespin: Boolean = false, //использовал фриспин или коин
    @SerializedName("visual_sequence") val visualSequence: List<PrizeItem>? = null, //анимационная последовательность призов для слотов
    @SerializedName("is_rare_spin") val isRareSpin: Boolean = false, //редкий прокрут
    val message: String? = null //сообщение от сервера
)

//один приз в рулетке
data class PrizeItem(
    val id: String, //строковый id приза
    val name: String, //название приза
    val icon: String, //иконка эмодзи
    val type: String, //тип: coins (коины) или special (особый приз)
    val value: Double //значение (для коинов — сколько, для особых — 0)
)

//ответ от /game/spin-history — история прокрутов
data class SpinHistoryResponse(
    val history: List<SpinHistoryItem> //список прошлых прокрутов
)

//одна запись в истории прокрутов
data class SpinHistoryItem(
    val id: Int? = null, //id записи в базе
    @SerializedName("prize_id") val prizeId: String, //id приза
    @SerializedName("prize_name") val prizeName: String, //название приза
    @SerializedName("prize_icon") val prizeIcon: String, //иконка приза
    @SerializedName("used_freespin") val usedFreespin: Boolean, //использовал фриспин
    @SerializedName("created_at") val createdAt: String //дата прокрута
)

//ответ от /game/special-prizes — особые призы
data class SpecialPrizesResponse(
    val active: List<SpecialPrizeItem>, //активные призы (можно применить или продать)
    val used: List<SpecialPrizeItem> //использованные призы
)

//один особый приз
data class SpecialPrizeItem(
    val id: Int, //id в базе (нужен для apply/sell)
    @SerializedName("prize_id") val prizeId: String, //строковый id типа приза
    @SerializedName("prize_name") val prizeName: String, //название
    @SerializedName("prize_icon") val prizeIcon: String, //иконка
    val status: String, //статус: active, used
    @SerializedName("created_at") val createdAt: String, //дата получения
    @SerializedName("used_at") val usedAt: String? = null //дата использования (если использован)
)

//ответ со списком заявок на призы
data class PrizeRequestsResponse(
    val requests: List<PrizeRequestItem> //список заявок
)

//одна заявка на приз
data class PrizeRequestItem(
    val id: Int, //id заявки
    @SerializedName("prize_id") val prizeId: String, //id приза
    @SerializedName("prize_name") val prizeName: String, //название
    val status: String,
    @SerializedName("created_at") val createdAt: String //когда подал заявку
)

//ответ со статистикой трат коинов
data class CoinsUsageResponse(
    val usage: Map<String, Double>? = null //ключ = категория трат, значение = потраченные коины
)

//тело запроса для обмена коинов на баллы
data class ExchangeCoinsRequest(
    val subject: String,
    val coins: Double
)
