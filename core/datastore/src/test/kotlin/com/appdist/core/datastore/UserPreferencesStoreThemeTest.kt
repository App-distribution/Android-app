package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.appdist.core.common.model.ThemeMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserPreferencesStoreThemeTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: UserPreferencesStore

    @BeforeEach
    fun setUp() {
        dataStore = mockk(relaxed = true)
        store = UserPreferencesStore(dataStore)
    }

    @Test
    fun `themeMode отдаёт LIGHT, когда ничего не сохранено`() = runTest {
        every { dataStore.data } returns flowOf(mutablePreferencesOf())
        store.themeMode.test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `themeMode читает сохранённое значение LIGHT`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "LIGHT")
        every { dataStore.data } returns flowOf(prefs)
        store.themeMode.test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `themeMode читает сохранённое значение DARK`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "DARK")
        every { dataStore.data } returns flowOf(prefs)
        store.themeMode.test {
            assertEquals(ThemeMode.DARK, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `themeMode откатывается к LIGHT при неизвестном значении`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "SEPIA")
        every { dataStore.data } returns flowOf(prefs)
        store.themeMode.test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `resolveDark учитывает системную настройку только в режиме SYSTEM`() {
        assertEquals(true, ThemeMode.SYSTEM.resolveDark(systemInDark = true))
        assertEquals(false, ThemeMode.SYSTEM.resolveDark(systemInDark = false))
        assertEquals(false, ThemeMode.LIGHT.resolveDark(systemInDark = true))
        assertEquals(true, ThemeMode.DARK.resolveDark(systemInDark = false))
    }
}
