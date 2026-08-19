package com.appdist.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.datastore.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
        // DataStoreModule не задаёт corruptionHandler: повреждённый файл настроек
        // роняет dataStore.data исключением. catch защищает только этот поток —
        // themeMode это get()-свойство, и каждый вызов создаёт новый Flow, так что
        // AppNavViewModel и SettingsViewModel собирают свои экземпляры без catch и
        // всё равно упадут при повреждённом сторе. Здесь мы лишь не даём гейту
        // первого кадра рухнуть и деградируем к системной теме вместо краха.
        .catch { emit(ThemeMode.SYSTEM) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
