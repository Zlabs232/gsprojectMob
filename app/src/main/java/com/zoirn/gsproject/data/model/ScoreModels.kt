package com.zoirn.gsproject.data.model

import com.google.gson.annotations.SerializedName

//ответ от /student/scores — всё про баллы студента
data class ScoresResponse(
    val stats: ScoreStats, //общая статистика
    val submissions: List<SubmissionItem>, //список сданных лаб
    @SerializedName("homework_grades") val homeworkGrades: List<HomeworkGrade> //оценки за домашки
)

//общая статистика по баллам
data class ScoreStats(
    @SerializedName("average_score") val averageScore: Double, //средний балл по всем предметам
    @SerializedName("total_score") val totalScore: Double, //сумма всех баллов
    @SerializedName("completed_works") val completedWorks: Int, //количество сданных работ
    @SerializedName("total_works") val totalWorks: Int, //всего работ (сданных + несданных)
    @SerializedName("isrpo_score") val isrpoScore: Double, //баллы по ИСРПО
    @SerializedName("isrpo_count") val isrpoCount: Int, //количество работ по ИСРПО
    @SerializedName("rmp_score") val rmpScore: Double, //баллы по РМП
    @SerializedName("rmp_count") val rmpCount: Int, //количество работ по РМП
    @SerializedName("homework_score") val homeworkScore: Double, //баллы за домашки
    @SerializedName("homework_count") val homeworkCount: Int //количество домашек
)

//одна сданная лаба
data class SubmissionItem(
    val title: String?, //название работы (может не быть)
    @SerializedName("max_score") val maxScore: Int?, //максимальный балл
    val score: Double?, //полученный балл (null если ещё не проверено)
    @SerializedName("submitted_at") val submittedAt: String?, //дата сдачи
    @SerializedName("checked_at") val checkedAt: String?, //дата проверки преподом
    @SerializedName("github_repo_url") val githubRepoUrl: String? //ссылка на репо
)

//оценка за одну домашку
data class HomeworkGrade(
    @SerializedName("homework_number") val homeworkNumber: Int, //номер домашки
    val score: Double?, //оценка за домашку
    val comment: String?, //комментарий препода
    @SerializedName("graded_at") val gradedAt: String?, //когда поставил оценку
    @SerializedName("stepik_certificate_url") val stepikCertificateUrl: String?, //ссылка на сертификат степика
    @SerializedName("submitted_at") val submittedAt: String? //когда сдал
)

//баллы за конкретный период (месяц/предмет)
data class PeriodScoresResponse(
    val period: String, //период
    val subject: String, //предмет
    val stats: PeriodStats, //статистика за период
    val submissions: List<SubmissionItem> //работы за этот период
)

//мини-статистика за период
data class PeriodStats(
    val average: Double, //средний балл
    val max: Double, //максимальный балл
    val min: Double, //минимальный балл
    val count: Int //количество работ
)

//ответ на поиск студентов
data class SearchStudentsResponse(
    val students: List<StudentSearchResult> //список найденных студентов
)

//один студент из результатов поиска
data class StudentSearchResult(
    val id: Int, //id в базе
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("middle_name") val middleName: String?,
    val nickname: String?, //никнейм
    @SerializedName("full_name") val fullName: String, //полное имя (собирается на сервере)
    val email: String?, //почта
    @SerializedName("group_name") val groupName: String?, //группа
    @SerializedName("avatar_url") val avatarUrl: String?, //аватарка
    @SerializedName("github_login") val githubLogin: String?, //логин на гитхабе
    val stats: ScoreStats? //статистика по баллам (если есть)
)

//ответ со списком студентов группы
data class GroupStudentsResponse(
    @SerializedName("group_name") val groupName: String, //название группы
    val subject: String, //предмет
    @SerializedName("average_score") val averageScore: Double, //средний балл по группе
    val students: List<GroupStudentItem> //список студентов
)

//один студент в таблице группы
data class GroupStudentItem(
    @SerializedName("full_name") val fullName: String, //полное имя
    @SerializedName("group_name") val groupName: String, //группа
    @SerializedName("average_score") val averageScore: Double, //средний балл студента
    @SerializedName("completed_works") val completedWorks: Int //сколько работ сдано
)
