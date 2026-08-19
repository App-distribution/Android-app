package com.appdist.app

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.app.navigation.AppNavGraph
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.volcanicDarkColors
import com.appdist.core.ui.theme.volcanicLightColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Сохранённый режим темы ещё не прочитан из DataStore на этом шаге — LaunchedEffect
        // ниже отработает позже, а до этого стартовое окно ОС и первый пустой кадр показывают
        // фон окна как есть. Ставим его по лучшей догадке, чтобы не мелькал платформенный фон;
        // когда придёт сохранённый выбор, LaunchedEffect поправит его, если тот разошёлся.
        // Догадка — режим по умолчанию, а не системная настройка: пока пользователь не выбрал
        // тему сам, приложение будет именно в нём, а системная ночная настройка сказала бы
        // светло на светлом телефоне и дала бы вспышку перед тёмным первым экраном.
        val guessedDark = ThemeMode.DEFAULT.resolveDark(
            systemInDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        )
        window.setBackgroundDrawable(
            ColorDrawable(
                (if (guessedDark) volcanicDarkColors() else volcanicLightColors()).ground.toArgb()
            )
        )
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val mode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = mode?.resolveDark(isSystemInDarkTheme()) ?: return@setContent

            LaunchedEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
                )
                val ground = if (darkTheme) volcanicDarkColors().ground else volcanicLightColors().ground
                window.setBackgroundDrawable(ColorDrawable(ground.toArgb()))
            }

            AppTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
    }
}
