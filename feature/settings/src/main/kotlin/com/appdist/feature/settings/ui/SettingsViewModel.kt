package com.appdist.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.datastore.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val actionZoneOnLeft: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DEFAULT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(prefsStore.actionZoneOnLeft, prefsStore.themeMode) { onLeft, mode ->
            SettingsUiState(actionZoneOnLeft = onLeft, themeMode = mode)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setActionZoneOnLeft(onLeft: Boolean) {
        viewModelScope.launch { prefsStore.setActionZoneOnLeft(onLeft) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsStore.setThemeMode(mode) }
    }
}
