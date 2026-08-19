package com.appdist.feature.settings

import app.cash.turbine.test
import com.appdist.core.common.model.ThemeMode
import com.appdist.core.datastore.UserPreferencesStore
import com.appdist.feature.settings.ui.SettingsViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var prefsStore: UserPreferencesStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefsStore = mockk(relaxed = true)
        every { prefsStore.actionZoneOnLeft } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `состояние отдаёт сохранённый режим темы`() = runTest(dispatcher) {
        every { prefsStore.themeMode } returns flowOf(ThemeMode.LIGHT)
        val viewModel = SettingsViewModel(prefsStore)
        viewModel.state.test {
            // WhileSubscribed(5_000) стартует апстрим только по первой подписке:
            // первый элемент — ещё дефолт из stateIn, второй — уже из хранилища.
            skipItems(1)
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode пишет выбор в хранилище`() = runTest(dispatcher) {
        every { prefsStore.themeMode } returns flowOf(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(prefsStore)
        viewModel.setThemeMode(ThemeMode.DARK)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { prefsStore.setThemeMode(ThemeMode.DARK) }
    }
}
