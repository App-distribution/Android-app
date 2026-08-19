package com.appdist.app

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
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.volcanicDarkColors
import com.appdist.core.ui.theme.volcanicLightColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
