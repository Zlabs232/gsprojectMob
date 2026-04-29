package com.zoirn.gsproject.data.model

import com.google.gson.annotations.SerializedName

//ответ от /student/repos — список репов студента
data class ReposResponse(
    val repos: List<RepoItem> //список репов с гитхаба
)

//один репозиторий с гитхаба
data class RepoItem(
    val name: String, //название репозитория
    val url: String, //ссылка на репу
    @SerializedName("updated_at") val updatedAt: String?, //дата последнего обновления
    val description: String?, //описание репы (может не быть)
    val language: String?, //яп
    val fork: Boolean = false, //форк или нет
    val score: Double?, //оценка за репозиторий
    val checked: Boolean = false //проверена ли преподом
)
