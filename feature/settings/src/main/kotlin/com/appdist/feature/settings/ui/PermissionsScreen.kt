package com.appdist.feature.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.components.ActionCaption
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.core.ui.components.GraphDestination
import com.appdist.core.ui.components.RailAction
import com.appdist.core.ui.components.VolcanicCard
import com.appdist.core.ui.components.VolcanicScaffold
import com.appdist.core.ui.components.bottomGapAfterInsets
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoLabel

@Composable
fun PermissionsScreen(
    actionZoneSide: ActionZoneSide = ActionZoneSide.Right,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val openSystemSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } else {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    VolcanicScaffold(
        side = actionZoneSide,
        destination = GraphDestination.Settings,
        zoneAction = {
            ActionCaption("ДЕЙСТВИЕ")
            Spacer(Modifier.height(8.dp))
            RailAction(
                icon = Icons.Default.SettingsApplications,
                label = "Открыть",
                onClick = openSystemSettings,
                minHeight = 126.dp,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 16.dp, bottom = bottomGapAfterInsets()),
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(48.dp)
                    .background(AppTheme.colors.panel, RoundedCornerShape(16.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Spacer(Modifier.height(14.dp))
            Text("Разрешение установки", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Android должен разрешить AppDist устанавливать тестовые APK.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted,
            )
            Spacer(Modifier.height(28.dp))
            Text("КАК ВКЛЮЧИТЬ", style = MonoLabel, color = AppTheme.colors.accent)
            Spacer(Modifier.height(12.dp))
            VolcanicCard(cornerRadius = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("1. Откройте системные настройки", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("2. Разрешите установку из этого источника", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("3. Вернитесь в AppDist", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
