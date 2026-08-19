package com.appdist.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.datastore.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val actionZoneOnLeft: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = prefsStore.actionZoneOnLeft
        .map { SettingsUiState(actionZoneOnLeft = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setActionZoneOnLeft(onLeft: Boolean) {
        viewModelScope.launch { prefsStore.setActionZoneOnLeft(onLeft) }
    }
}
