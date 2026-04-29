package com.zoirn.gsproject.data.model

import com.google.gson.annotations.SerializedName

//ответ от /student/notifications — список уведомлений
data class NotificationsResponse(
    val notifications: List<NotificationItem> //список уведомлений
)

//одно уведомление
data class NotificationItem(
    val id: Int, //id уведомления (нужен для пометки как прочитанное)
    val title: String, //заголовок уведомления
    val message: String, //текст уведомления
    @SerializedName("created_at") val createdAt: String, //дата создания
    @SerializedName("is_read") val isRead: Int //0 = непрочитанное, 1 = прочитанное
)

//тело запроса для пометки уведомления прочитанным
data class MarkReadRequest(
    @SerializedName("notification_id") val notificationId: Int //id уведомления которое читает
)

//ответ от /student/homework — список домашек
data class HomeworkResponse(
    val homework: List<HomeworkItem> //список домашек студента
)

//одна домашка
data class HomeworkItem(
    val id: Int? = null, //id в базе
    @SerializedName("homework_number") val homeworkNumber: Int, //номер домашки
    @SerializedName("stepik_certificate_url") val stepikCertificateUrl: String?, //ссылка на сертификат степика
    @SerializedName("submitted_at") val submittedAt: String?, //когда сдал
    val status: String, //статус: submitted (на проверке) или graded (оценено)
    val score: Double?, //оценка
    val comment: String?, //коммет преподавателя
    @SerializedName("graded_at") val gradedAt: String? //когда поставили оценку
)

//тело запроса для сдачи домашней работы
data class HomeworkSubmitRequest(
    @SerializedName("homework_number") val homeworkNumber: Int, //номер домашки
    @SerializedName("stepik_certificate_url") val stepikCertificateUrl: String, //ссылка на сертификат
    val comment: String? = null //коммент студента
)
