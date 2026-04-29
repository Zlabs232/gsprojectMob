package com.zoirn.gsproject.data.api

import com.zoirn.gsproject.data.model.*
import retrofit2.Response
import retrofit2.http.*

//интерфейс всех http-запросов к серверу через ретрофит (каждая функция = один запрос)
interface ApiService {

    //везде http запросы с корутиной к беку через ретрофит
    // === Auth ===
    @GET("auth/me") //GET запрос — получить инфу о текущем пользователе
    suspend fun getCurrentUser(): Response<UserResponse>

    // === Profile ===
    @GET("student/profile") //получить профиль студента
    suspend fun getStudentProfile(): Response<ProfileResponse>

    @POST("profile") //сохранить/обновить профиль студента
    suspend fun saveProfile(@Body profile: ProfileRequest): Response<StatusResponse>

    // === Repos ===
    @GET("student/repos") //получить список репозиториев студента с гитхаба
    suspend fun getStudentRepos(
        @Query("force_refresh") forceRefresh: Boolean = false //параметр: принудительно обновить кЕш
    ): Response<ReposResponse>

    // === Scores ===
    @GET("student/scores") //получить все баллы студента
    suspend fun getStudentScores(): Response<ScoresResponse>

    @GET("student/scores/period") //получить баллы за конкретный месяц/предмет
    suspend fun getScoresByPeriod(
        @Query("month") month: Int,
        @Query("year") year: Int = 2026,
        @Query("subject") subject: String? = null //предмет (null = все предметы)
    ): Response<PeriodScoresResponse>

    // === Notifications ===
    @GET("student/notifications") //получить список уведомлений студента
    suspend fun getNotifications(): Response<NotificationsResponse>

    @POST("student/notifications/mark-read") //пометить уведомление как прочитанное
    suspend fun markNotificationRead(@Body body: MarkReadRequest): Response<StatusResponse>

    // === Homework ===
    @GET("student/homework") //получить список домашних работ
    suspend fun getHomework(): Response<HomeworkResponse>

    @POST("student/homework/submit") //отправить домашнюю работу на проверку
    suspend fun submitHomework(@Body body: HomeworkSubmitRequest): Response<StatusResponse>

    // === Game ===
    @GET("game/balance") //получить баланс коинов и фриспинов
    suspend fun getGameBalance(): Response<BalanceResponse>

    @POST("game/daily-freespin") //забрать ежедневный фриспин
    suspend fun claimDailyFreespin(): Response<FreespinResponse>

    @GET("game/weekly-tasks") //получить задания на текущую неделю
    suspend fun getWeeklyTasks(): Response<WeeklyTasksResponse>

    @POST("game/check-tasks") //проверить выполнение заданий и начислить коины
    suspend fun checkWeeklyTasks(): Response<CheckTasksResponse>

    @POST("game/spin") //крутануть рулетку
    suspend fun spin(): Response<SpinResultResponse>

    @GET("game/spin-history") //получить историю прокрутов рулетки
    suspend fun getSpinHistory(): Response<SpinHistoryResponse>

    @GET("game/special-prizes") //получить список выбитых специальных призов
    suspend fun getSpecialPrizes(): Response<SpecialPrizesResponse>

    @POST("game/special-prizes/{id}/request") //отправить заявку на применение приза
    suspend fun requestPrize(@Path("id") prizeId: Int): Response<StatusResponse>

    @POST("game/special-prizes/{id}/sell") //продать приз за коины
    suspend fun sellPrize(@Path("id") prizeId: Int): Response<StatusResponse>

    @GET("game/my-prize-requests") //получить мои заявки на призы
    suspend fun getMyPrizeRequests(): Response<PrizeRequestsResponse>

    @GET("game/coins-usage") //получить статистику трат коинов
    suspend fun getCoinsUsage(): Response<CoinsUsageResponse>

    @POST("game/exchange-coins") //обменять коины на баллы по предмету
    suspend fun exchangeCoins(@Body body: ExchangeCoinsRequest): Response<StatusResponse>

    // === Search & Groups ===
    @GET("student/search") //поиск студента по ФИО или гитхабь логину
    suspend fun searchStudent(@Query("search_query") query: String): Response<SearchStudentsResponse>

    @GET("students/group") //получить всех студентов группы по предмету
    suspend fun getGroupStudents(
        @Query("group_name") groupName: String,
        @Query("subject") subject: String? = null
    ): Response<GroupStudentsResponse>

    // === Events ===
    @POST("student/event") //залогировать событие (для аналитики — какой экран открыл)
    suspend fun logEvent(@Body event: StudentEventRequest): Response<StatusResponse>
}
