package com.zoirn.gsproject.data.api

import com.zoirn.gsproject.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

//синглтон — создаётся один раз при запуске и живёт до конца сессии
object RetrofitClient {

    //токен сессии (берётся из куки после авторизации)
    private var sessionToken: String? = null
    //хранилище куки по доменам (ключ = хост, значение = список куки)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    //сохранить токен сессии в памяти (вызывается после логина)
    fun setSessionToken(token: String?) {
        sessionToken = token
    }

    //получить текущий токен сессии
    fun getSessionToken(): String? = sessionToken

    //логгер http-запросов: в дебаге показывает тело запроса/ответа, в релизе — молчит
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY //полный лог в дебаге
        } else {
            HttpLoggingInterceptor.Level.NONE //ничего не логируем в релизе
        }
    }

    //менеджер куки — сохраняет куки от сервера и подставляет при следующих запросах
    private val cookieJar = object : CookieJar {
        //вызывается когда сервер прислал куки в ответе
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host //берём домен из урла
            cookieStore.getOrPut(host) { mutableListOf() }.apply {
                //заменяем старую куку с таким же именем на новую
                cookies.forEach { newCookie ->
                    removeAll { it.name == newCookie.name }
                    add(newCookie)
                }
            }
            //если среди кук есть session_token — сохраняем в переменную
            cookies.find { it.name == "session_token" }?.let {
                sessionToken = it.value
            }
        }

        //вызывается перед каждым запросом — подставляем нужные куки
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host]?.toMutableList() ?: mutableListOf()
            //если у нас есть токен но его нет в куках — добавляем вручную
            sessionToken?.let { token ->
                if (cookies.none { it.name == "session_token" }) {
                    val cookie = Cookie.Builder()
                        .domain(url.host) //домен куки
                        .path("/") //путь — для всего сайта
                        .name("session_token") //имя куки
                        .value(token) //значение — токен
                        .build()
                    cookies.add(cookie)
                }
            }
            return cookies
        }
    }

    //http-клиент с настройками таймаутов и логгером
    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar) //менеджер куки
        .addInterceptor(loggingInterceptor) //логгер запросов
        .connectTimeout(15, TimeUnit.SECONDS) //таймаут подключения
        .readTimeout(15, TimeUnit.SECONDS) //таймаут чтения ответа
        .writeTimeout(15, TimeUnit.SECONDS) //таймаут отправки данных
        .build()

    //ретрофит — обёртка над http-клиентом, превращает интерфейс в реальные запросы
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL) //базовый урл сервера из конфига
        .client(okHttpClient) //используем наш настроенный клиент
        .addConverterFactory(GsonConverterFactory.create()) //json <-> kotlin объекты через gson
        .build()

    //готовый сервис для запросов — использую везде в приложении
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
