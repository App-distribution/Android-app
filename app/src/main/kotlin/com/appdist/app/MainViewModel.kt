package com.appdist.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.datastore.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Режим темы читается до первого кадра: null означает «ещё не прочитали»,
 * и в этот момент контент не рисуется, иначе мелькнёт чужой фон.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    prefsStore: UserPreferencesStore
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = prefsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
