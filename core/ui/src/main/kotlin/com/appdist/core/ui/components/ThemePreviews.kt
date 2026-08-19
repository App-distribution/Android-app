package com.appdist.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.AppTheme

/**
 * Витрина компонентов для сверки светлой и тёмной палитры. Не часть
 * рабочего графа экранов — только для просмотра в Android Studio.
 */
@Preview(name = "Светлая", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Тёмная", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SurfacesPreview() {
    AppTheme {
        Column(
            Modifier
                .background(AppTheme.colors.ground)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            VolcanicCard {
                Text("Панель", color = AppTheme.colors.ink)
                Text("Подпись", color = AppTheme.colors.muted)
                Text("Мелкая подпись", color = AppTheme.colors.mutedDim)
            }
            PanelDivider(Modifier.padding(vertical = 12.dp))
            StorageBar(fraction = .37f)
            HandednessPreview(onLeft = false, modifier = Modifier.padding(top = 12.dp))
            BuildStatusChip(label = "mandatory", type = BuildChipType.MANDATORY, modifier = Modifier.padding(top = 12.dp))
            BuildStatusChip(label = "latest", type = BuildChipType.LATEST, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
