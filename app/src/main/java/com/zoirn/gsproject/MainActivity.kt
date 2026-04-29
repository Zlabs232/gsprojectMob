package com.zoirn.gsproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.zoirn.gsproject.data.api.RetrofitClient
import com.zoirn.gsproject.navigation.AppNavGraph
import com.zoirn.gsproject.navigation.Routes
import com.zoirn.gsproject.ui.theme.GsProjectTheme
import com.zoirn.gsproject.utils.AppTheme
import com.zoirn.gsproject.utils.SessionManager
import com.zoirn.gsproject.utils.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // инит сессии
        SessionManager.init(this)

        // бурмалда с сессией(был юзер -> сохр)
        SessionManager.getSessionToken()?.let { token ->
            RetrofitClient.setSessionToken(token)
        }

        //ui шляпа для отрисовки без отступов
        enableEdgeToEdge()

        //ключ компоуза, описывается ui не через xml, а через функцию
        setContent {
            //хранение темы
            val appTheme by ThemePreferences.getTheme(this)
                .collectAsState(initial = AppTheme.DARK)

            //авторизация в приложен(есть/нет юзера)
            GsProjectTheme(appTheme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val startDestination = if (SessionManager.isLoggedIn()) {
                        Routes.MAIN
                    } else {
                        Routes.LOGIN
                    }

                    //список всех экранов и переходов между ними
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
