package com.zoirn.gsproject.data.model

import com.google.gson.annotations.SerializedName

//модель ответа от /auth/me — данные о текущем пользователе
data class UserResponse(
    val id: Int,
    @SerializedName("github_login") val githubLogin: String,
    val email: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val role: String,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("middle_name") val middleName: String?,
    @SerializedName("group_name") val groupName: String?
)

//модель ответа от /student/profile — полный профиль студента
data class ProfileResponse(
    @SerializedName("github_login") val githubLogin: String,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("middle_name") val middleName: String?,
    val nickname: String?,
    val email: String?,
    @SerializedName("group_name") val groupName: String?,
    @SerializedName("avatar_url") val avatarUrl: String?
)

//тело запроса для сохранения профиля — отправляю на сервер
data class ProfileRequest(
    @SerializedName("github_login") val githubLogin: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("middle_name") val middleName: String?,
    val nickname: String?,
    @SerializedName("group_name") val groupName: String
)

//универсальный ответ сервера на действия (сохранить, удалить и т.д.)
data class StatusResponse(
    val status: String? = null,
    val message: String? = null,
    val success: Boolean? = null,
    val ok: Boolean? = null
)

//тело запроса для логирования события (какой экран открыл, что нажал)
data class StudentEventRequest(
    @SerializedName("github_login") val githubLogin: String, //чей логин
    val action: String,
    val label: String? = null
)
