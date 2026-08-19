package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appdist.core.common.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_ACTION_ZONE_ON_LEFT = booleanPreferencesKey("action_zone_on_left")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val actionZoneOnLeft: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACTION_ZONE_ON_LEFT] ?: false
    }

    val themeMode: Flow<ThemeMode>
        get() = dataStore.data.map { prefs ->
            ThemeMode.fromStorage(prefs[KEY_THEME_MODE])
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setActionZoneOnLeft(onLeft: Boolean) {
        dataStore.edit { it[KEY_ACTION_ZONE_ON_LEFT] = onLeft }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }
}
