# Светлая тема Volcanic Split — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Приложение получает светлую тему по макету `Volcanic Split Flow · Light Theme` и выбор режима «Как в системе / Светлая / Тёмная» в настройках.

**Architecture:** Плоские top-level константы `Volcanic*` заменяются на `AppColors` — иммутабельный набор из 37 токенов, раздаваемый через `staticCompositionLocalOf`. Две палитры (`volcanicDarkColors()`, `volcanicLightColors()`); из выбранной выводится `MaterialTheme.colorScheme`, чтобы дефолты Material 3 были корректны в обеих темах. Режим темы хранится в DataStore и применяется в `MainActivity`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, DataStore Preferences, JUnit5 + MockK + Turbine.

**Spec:** `docs/superpowers/specs/2026-08-18-light-theme-design.md`

## Global Constraints

- **Тёмная тема не меняется ни в одном пикселе.** Значения тёмной палитры переносятся байт в байт из `core/ui/src/main/kotlin/com/appdist/core/ui/theme/Color.kt`. Это проверяется тестом в Task 2.
- **Модуль Android-приложения:** все пути относительно `/Users/emalovichko/StudioProjects/app_distribution/.worktrees/android-app`.
- **Сборка:** `./gradlew assembleDebug`; тесты модуля: `./gradlew :<модуль>:testDebugUnitTest`.
- **JVM 17, minSdk 24, compileSdk 35.** Тесты — JUnit5 (`useJUnitPlatform()`), ассерты из `org.junit.jupiter.api.Assertions`.
- **Язык:** комментарии, сообщения коммитов и тексты UI — русские, в тон существующему коду.
- **Формат коммитов:** `<type>(<scope>): <описание в повелительном наклонении>`, например `feat(theme): ввести токены AppColors`.
- **Токен-таблица** (единственный источник значений для всех задач):

| Токен | Dark | Light |
|---|---|---|
| `ground` | `0xFF080B0C` | `0xFFF8FAF7` |
| `surface` | `0xFF111718` | `0xFFFFFFFF` |
| `surfaceRaised` | `0xFF182123` | `0xFFEFF5F2` |
| `panel` | `0xCC10191A` | `0xF2FFFFFF` |
| `panelStroke` | `0x55FFFFFF` | `0xFFAFC3BD` |
| `panelStrokeStrong` | `0x66FFFFFF` | `0xFF9DB2AC` |
| `field` | `0x99050808` | `0xFFFFFFFF` |
| `chip` | `0xAA050808` | `0xFFFFFFFF` |
| `track` | `0xFF263637` | `0xFFC8D6D1` |
| `outline` | `0xFF30433F` | `0xFFAFC3BD` |
| `ink` | `0xFFECF1EF` | `0xFF132D29` |
| `inkSoft` | `0xFFD5E0DD` | `0xFF334D47` |
| `muted` | `0xFFA9BCB8` | `0xFF516A65` |
| `mutedDim` | `0xFF8FA7A3` | `0xFF5B736D` |
| `ghost` | `0x08FFFFFF` | `0x0D0B3F3C` |
| `ghostLabel` | `0x33AFC5C0` | `0x665B736D` |
| `accent` | `0xFF9DDDD2` | `0xFF0B756C` |
| `accentBright` | `0xFFC9FFF5` | `0xFFF4FFFC` |
| `action` | `0xFF063F47` | `0xFFD6ECE7` |
| `actionTile` | `0xFFECF1EF` | `0xFF132D29` |
| `onActionTile` | `0xFF063F47` | `0xFFFFFFFF` |
| `railTarget` | `0xFF052F35` | `0xFFC4E0DA` |
| `railTargetStroke` | `0x33FFFFFF` | `0xFFC5D3CF` |
| `railDivider` | `0x66FFFFFF` | `0xFF9DB2AC` |
| `graph` | `0x387FA09A` | `0x3F819C96` |
| `nodeRelated` | `0xFF376C67` | `0xFF84B6AD` |
| `nodeOutline` | `0x777FA09A` | `0x8A6F8F88` |
| `previewGround` | `0x99050808` | `0xFFEFF5F2` |
| `actionPreview` | `0xFF0C6974` | `0xFF0B756C` |
| `statusMandatory` | `0xFFFFB4AB` | `0xFF8C1C00` |
| `statusMandatoryContainer` | `0xFF4A1D1B` | `0xFFF0DEDA` |
| `statusDeprecated` | `0xFFCAD4D1` | `0xFF4A5754` |
| `statusDeprecatedContainer` | `0xFF29302F` | `0xFFE4E9E7` |
| `statusExpiring` | `0xFFFFD18A` | `0xFF804200` |
| `statusExpiringContainer` | `0xFF4A3210` | `0xFFF2E6D3` |
| `statusLatest` | `0xFF9DDDD2` | `0xFF0B756C` |
| `statusLatestContainer` | `0xFF063F47` | `0xFFD6ECE7` |

- **Правило замены при миграции** (Task 3-6): `VolcanicXxx` → `AppTheme.colors.xxx` с первой буквой в нижнем регистре (`VolcanicGround` → `AppTheme.colors.ground`, `VolcanicPanelStroke` → `AppTheme.colors.panelStroke`). Исключения перечислены поимённо в своих задачах. Импорты `com.appdist.core.ui.theme.Volcanic*` удаляются, добавляется `com.appdist.core.ui.theme.AppTheme`.
- **Важно про `AppTheme.colors`:** это `@Composable`-геттер. Он читается только внутри composable-функций. В `DrawScope`-блоках (`Canvas`, `drawBehind`) цвет нужно поднять в переменную до блока рисования — примеры даны в Task 3.

---

### Task 1: Режим темы в настройках пользователя

**Files:**
- Create: `core/common/src/main/kotlin/com/appdist/core/common/model/ThemeMode.kt`
- Modify: `core/datastore/build.gradle.kts`
- Modify: `core/datastore/src/main/kotlin/com/appdist/core/datastore/UserPreferencesStore.kt`
- Test: `core/datastore/src/test/kotlin/com/appdist/core/datastore/UserPreferencesStoreThemeTest.kt`

**Interfaces:**
- Consumes: ничего (первая задача).
- Produces: `enum class ThemeMode { SYSTEM, LIGHT, DARK }` с методом `fun resolveDark(systemInDark: Boolean): Boolean` и `companion object { fun fromStorage(raw: String?): ThemeMode }`; `UserPreferencesStore.themeMode: Flow<ThemeMode>` и `suspend fun UserPreferencesStore.setThemeMode(mode: ThemeMode)`.

- [ ] **Step 1: Написать падающий тест**

Создать `core/datastore/src/test/kotlin/com/appdist/core/datastore/UserPreferencesStoreThemeTest.kt`:

```kotlin
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
    fun `themeMode отдаёт SYSTEM, когда ничего не сохранено`() = runTest {
        every { dataStore.data } returns flowOf(mutablePreferencesOf())
        store.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
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
    fun `themeMode откатывается к SYSTEM при неизвестном значении`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "SEPIA")
        every { dataStore.data } returns flowOf(prefs)
        store.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
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
```

- [ ] **Step 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew :core:datastore:testDebugUnitTest --tests "*UserPreferencesStoreThemeTest*"`
Expected: FAIL — компиляция не проходит, `Unresolved reference: ThemeMode`.

- [ ] **Step 3: Создать ThemeMode**

Создать `core/common/src/main/kotlin/com/appdist/core/common/model/ThemeMode.kt`:

```kotlin
package com.appdist.core.common.model

/** Режим оформления, выбранный пользователем в настройках. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Тёмная ли тема при текущей системной настройке. */
    fun resolveDark(systemInDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Значение из хранилища: неизвестное трактуем как SYSTEM. */
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
```

- [ ] **Step 4: Дать datastore доступ к core:common**

В `core/datastore/build.gradle.kts` в блок `dependencies` первой строкой добавить:

```kotlin
    implementation(project(":core:common"))
```

- [ ] **Step 5: Добавить чтение и запись режима**

В `core/datastore/src/main/kotlin/com/appdist/core/datastore/UserPreferencesStore.kt` добавить импорты:

```kotlin
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appdist.core.common.model.ThemeMode
```

В `companion object` добавить ключ:

```kotlin
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
```

После `actionZoneOnLeft` добавить поток и сеттер:

```kotlin
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[KEY_THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }
```

- [ ] **Step 6: Запустить тест и убедиться, что он проходит**

Run: `./gradlew :core:datastore:testDebugUnitTest --tests "*UserPreferencesStoreThemeTest*"`
Expected: PASS, 5 тестов.

- [ ] **Step 7: Коммит**

```bash
git add core/common/src/main/kotlin/com/appdist/core/common/model/ThemeMode.kt \
        core/datastore/build.gradle.kts \
        core/datastore/src/main/kotlin/com/appdist/core/datastore/UserPreferencesStore.kt \
        core/datastore/src/test/kotlin/com/appdist/core/datastore/UserPreferencesStoreThemeTest.kt
git commit -m "feat(theme): хранить выбранный режим оформления"
```

---

### Task 2: Токены AppColors и две палитры

**Files:**
- Create: `core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppColors.kt`
- Modify: `core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppTheme.kt`
- Modify: `core/ui/build.gradle.kts`
- Test: `core/ui/src/test/kotlin/com/appdist/core/ui/theme/AppColorsTest.kt`
- Test: `core/ui/src/test/kotlin/com/appdist/core/ui/theme/LightPaletteContrastTest.kt`

**Interfaces:**
- Consumes: ничего из Task 1.
- Produces: `data class AppColors` с 37 полями (имена — левая колонка токен-таблицы); `fun volcanicDarkColors(): AppColors`; `fun volcanicLightColors(): AppColors`; `val LocalAppColors: ProvidableCompositionLocal<AppColors>`; `object AppTheme { val colors: AppColors }`; `@Composable fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.

**Важно:** старый `Color.kt` в этой задаче не трогаем — экраны продолжают на него ссылаться и всё компилируется. Он удаляется в Task 6.

- [ ] **Step 1: Подключить тесты в core:ui**

В `core/ui/build.gradle.kts` в конец блока `dependencies` добавить:

```kotlin
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
```

И в конец файла, после блока `dependencies`:

```kotlin
tasks.withType<Test> { useJUnitPlatform() }
```

- [ ] **Step 2: Написать падающие тесты**

Создать `core/ui/src/test/kotlin/com/appdist/core/ui/theme/AppColorsTest.kt` — снимок тёмной палитры, страхующий обещание «тёмная тема не меняется»:

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Снимок тёмной палитры. Значения перенесены из Color.kt до миграции на токены:
 * тест падает, если тёмная тема поехала при правках светлой.
 */
class AppColorsTest {

    @Test
    fun `тёмная палитра совпадает со снимком`() {
        val c = volcanicDarkColors()
        assertEquals(Color(0xFF080B0C), c.ground)
        assertEquals(Color(0xFF111718), c.surface)
        assertEquals(Color(0xFF182123), c.surfaceRaised)
        assertEquals(Color(0xCC10191A), c.panel)
        assertEquals(Color(0x55FFFFFF), c.panelStroke)
        assertEquals(Color(0x66FFFFFF), c.panelStrokeStrong)
        assertEquals(Color(0x99050808), c.field)
        assertEquals(Color(0xAA050808), c.chip)
        assertEquals(Color(0xFF263637), c.track)
        assertEquals(Color(0xFF30433F), c.outline)
        assertEquals(Color(0xFFECF1EF), c.ink)
        assertEquals(Color(0xFFD5E0DD), c.inkSoft)
        assertEquals(Color(0xFFA9BCB8), c.muted)
        assertEquals(Color(0xFF8FA7A3), c.mutedDim)
        assertEquals(Color(0x08FFFFFF), c.ghost)
        assertEquals(Color(0x33AFC5C0), c.ghostLabel)
        assertEquals(Color(0xFF9DDDD2), c.accent)
        assertEquals(Color(0xFFC9FFF5), c.accentBright)
        assertEquals(Color(0xFF063F47), c.action)
        assertEquals(Color(0xFFECF1EF), c.actionTile)
        assertEquals(Color(0xFF063F47), c.onActionTile)
        assertEquals(Color(0xFF052F35), c.railTarget)
        assertEquals(Color(0x33FFFFFF), c.railTargetStroke)
        assertEquals(Color(0x66FFFFFF), c.railDivider)
        assertEquals(Color(0x387FA09A), c.graph)
        assertEquals(Color(0xFF376C67), c.nodeRelated)
        assertEquals(Color(0x777FA09A), c.nodeOutline)
        assertEquals(Color(0x99050808), c.previewGround)
        assertEquals(Color(0xFF0C6974), c.actionPreview)
        assertEquals(Color(0xFFFFB4AB), c.statusMandatory)
        assertEquals(Color(0xFF4A1D1B), c.statusMandatoryContainer)
        assertEquals(Color(0xFFCAD4D1), c.statusDeprecated)
        assertEquals(Color(0xFF29302F), c.statusDeprecatedContainer)
        assertEquals(Color(0xFFFFD18A), c.statusExpiring)
        assertEquals(Color(0xFF4A3210), c.statusExpiringContainer)
        assertEquals(Color(0xFF9DDDD2), c.statusLatest)
        assertEquals(Color(0xFF063F47), c.statusLatestContainer)
    }

    @Test
    fun `светлая палитра отличается от тёмной по фону и тексту`() {
        val dark = volcanicDarkColors()
        val light = volcanicLightColors()
        assertEquals(Color(0xFFF8FAF7), light.ground)
        assertEquals(Color(0xFF132D29), light.ink)
        assertEquals(Color(0xFF0B756C), light.accent)
        assert(light.ground != dark.ground)
        assert(light.ink != dark.ink)
    }
}
```

Создать `core/ui/src/test/kotlin/com/appdist/core/ui/theme/LightPaletteContrastTest.kt`:

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Контраст по WCAG 2.1. Порог 4.5:1 — мелкий текст (AA), 3:1 — крупный текст
 * и границы. Полупрозрачные токены сначала накладываются на свой фон.
 */
class LightPaletteContrastTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background).luminance().toDouble()
        val bg = background.luminance().toDouble()
        val lighter = maxOf(fg, bg)
        val darker = minOf(fg, bg)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun `текст светлой темы проходит AA`() {
        val c = volcanicLightColors()
        val panelOverGround = c.panel.compositeOver(c.ground)
        assertTrue(contrast(c.ink, c.ground) >= 4.5) { "ink на ground" }
        assertTrue(contrast(c.ink, panelOverGround) >= 4.5) { "ink на panel" }
        assertTrue(contrast(c.inkSoft, panelOverGround) >= 4.5) { "inkSoft на panel" }
        assertTrue(contrast(c.muted, c.ground) >= 4.5) { "muted на ground" }
        assertTrue(contrast(c.mutedDim, c.ground) >= 4.5) { "mutedDim на ground" }
        assertTrue(contrast(c.accent, c.ground) >= 4.5) { "accent на ground" }
        assertTrue(contrast(c.onActionTile, c.actionTile) >= 4.5) { "текст на CTA-плитке" }
        assertTrue(contrast(c.ink, c.action) >= 4.5) { "ink на teal-рельсе" }
    }

    @Test
    fun `статусные чипы светлой темы проходят AA`() {
        val c = volcanicLightColors()
        assertTrue(contrast(c.statusMandatory, c.statusMandatoryContainer) >= 4.5)
        assertTrue(contrast(c.statusDeprecated, c.statusDeprecatedContainer) >= 4.5)
        assertTrue(contrast(c.statusExpiring, c.statusExpiringContainer) >= 4.5)
        assertTrue(contrast(c.statusLatest, c.statusLatestContainer) >= 4.5)
    }

    @Test
    fun `границы светлой темы различимы на своём фоне`() {
        val c = volcanicLightColors()
        assertTrue(contrast(c.panelStroke, c.ground) >= 1.3) { "stroke панели на фоне" }
        assertTrue(contrast(c.track, c.ground) >= 1.3) { "трек на фоне" }
    }
}
```

- [ ] **Step 3: Запустить тесты и убедиться, что они падают**

Run: `./gradlew :core:ui:testDebugUnitTest`
Expected: FAIL — `Unresolved reference: volcanicDarkColors`.

- [ ] **Step 4: Создать AppColors и палитры**

Создать `core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppColors.kt`:

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Фирменные токены Volcanic Split. Material 3 не даёт слотов под граф
 * навигации, ghost-цифры и панели, поэтому цвет живёт здесь, а colorScheme
 * выводится отсюда же — см. AppTheme.
 */
@Immutable
data class AppColors(
    // поверхности
    val ground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val panel: Color,
    val panelStroke: Color,
    val panelStrokeStrong: Color,
    val field: Color,
    val chip: Color,
    val track: Color,
    val outline: Color,
    // текст
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val mutedDim: Color,
    val ghost: Color,
    val ghostLabel: Color,
    // акцент и действие
    val accent: Color,
    val accentBright: Color,
    val action: Color,
    val actionTile: Color,
    val onActionTile: Color,
    // рельс и граф навигации
    val railTarget: Color,
    val railTargetStroke: Color,
    val railDivider: Color,
    val graph: Color,
    val nodeRelated: Color,
    val nodeOutline: Color,
    // превью хвата в настройках
    val previewGround: Color,
    val actionPreview: Color,
    // статусы сборок
    val statusMandatory: Color,
    val statusMandatoryContainer: Color,
    val statusDeprecated: Color,
    val statusDeprecatedContainer: Color,
    val statusExpiring: Color,
    val statusExpiringContainer: Color,
    val statusLatest: Color,
    val statusLatestContainer: Color,
)

/** Чёрная базальтовая земля, teal-шов действия, мятный свет маршрута. */
fun volcanicDarkColors(): AppColors = AppColors(
    ground = Color(0xFF080B0C),
    surface = Color(0xFF111718),
    surfaceRaised = Color(0xFF182123),
    panel = Color(0xCC10191A),
    panelStroke = Color(0x55FFFFFF),
    panelStrokeStrong = Color(0x66FFFFFF),
    field = Color(0x99050808),
    chip = Color(0xAA050808),
    track = Color(0xFF263637),
    outline = Color(0xFF30433F),
    ink = Color(0xFFECF1EF),
    inkSoft = Color(0xFFD5E0DD),
    muted = Color(0xFFA9BCB8),
    mutedDim = Color(0xFF8FA7A3),
    ghost = Color(0x08FFFFFF),
    ghostLabel = Color(0x33AFC5C0),
    accent = Color(0xFF9DDDD2),
    accentBright = Color(0xFFC9FFF5),
    action = Color(0xFF063F47),
    actionTile = Color(0xFFECF1EF),
    onActionTile = Color(0xFF063F47),
    railTarget = Color(0xFF052F35),
    railTargetStroke = Color(0x33FFFFFF),
    railDivider = Color(0x66FFFFFF),
    graph = Color(0x387FA09A),
    nodeRelated = Color(0xFF376C67),
    nodeOutline = Color(0x777FA09A),
    previewGround = Color(0x99050808),
    actionPreview = Color(0xFF0C6974),
    statusMandatory = Color(0xFFFFB4AB),
    statusMandatoryContainer = Color(0xFF4A1D1B),
    statusDeprecated = Color(0xFFCAD4D1),
    statusDeprecatedContainer = Color(0xFF29302F),
    statusExpiring = Color(0xFFFFD18A),
    statusExpiringContainer = Color(0xFF4A3210),
    statusLatest = Color(0xFF9DDDD2),
    statusLatestContainer = Color(0xFF063F47),
)

/**
 * Минерально-светлая поверхность: тот же маршрут и teal-зона, но основное
 * действие становится тёмной плиткой, а не светлой.
 */
fun volcanicLightColors(): AppColors = AppColors(
    ground = Color(0xFFF8FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEFF5F2),
    panel = Color(0xF2FFFFFF),
    panelStroke = Color(0xFFAFC3BD),
    panelStrokeStrong = Color(0xFF9DB2AC),
    field = Color(0xFFFFFFFF),
    chip = Color(0xFFFFFFFF),
    track = Color(0xFFC8D6D1),
    outline = Color(0xFFAFC3BD),
    ink = Color(0xFF132D29),
    inkSoft = Color(0xFF334D47),
    muted = Color(0xFF516A65),
    mutedDim = Color(0xFF5B736D),
    ghost = Color(0x0D0B3F3C),
    ghostLabel = Color(0x665B736D),
    accent = Color(0xFF0B756C),
    accentBright = Color(0xFFF4FFFC),
    action = Color(0xFFD6ECE7),
    actionTile = Color(0xFF132D29),
    onActionTile = Color(0xFFFFFFFF),
    railTarget = Color(0xFFC4E0DA),
    railTargetStroke = Color(0xFFC5D3CF),
    railDivider = Color(0xFF9DB2AC),
    graph = Color(0x3F819C96),
    nodeRelated = Color(0xFF84B6AD),
    nodeOutline = Color(0x8A6F8F88),
    previewGround = Color(0xFFEFF5F2),
    actionPreview = Color(0xFF0B756C),
    statusMandatory = Color(0xFF8C1C00),
    statusMandatoryContainer = Color(0xFFF0DEDA),
    statusDeprecated = Color(0xFF4A5754),
    statusDeprecatedContainer = Color(0xFFE4E9E7),
    statusExpiring = Color(0xFF804200),
    statusExpiringContainer = Color(0xFFF2E6D3),
    statusLatest = Color(0xFF0B756C),
    statusLatestContainer = Color(0xFFD6ECE7),
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors не предоставлены — оберните контент в AppTheme")
}

/** Точка доступа к токенам: AppTheme.colors.ink и т.д. */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
```

- [ ] **Step 5: Переписать AppTheme**

Заменить содержимое `core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppTheme.kt` целиком:

```kotlin
package com.appdist.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Схема Material выводится из фирменных токенов: тогда компоненты M3 без
 * явно заданного цвета попадают в тему, а не в дефолтную палитру Material.
 */
private fun AppColors.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = ground,
        primaryContainer = action,
        onPrimaryContainer = ink,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = ink,
        background = ground,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outline,
    )
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) volcanicDarkColors() else volcanicLightColors()

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = AppTypography,
            content = content
        )
    }
}
```

- [ ] **Step 6: Запустить тесты и убедиться, что они проходят**

Run: `./gradlew :core:ui:testDebugUnitTest`
Expected: PASS, 5 тестов (2 в `AppColorsTest`, 3 в `LightPaletteContrastTest`).

Если `LightPaletteContrastTest` падает на `Color`/`luminance` с ошибкой вида «not mocked», добавить в `core/ui/build.gradle.kts` в блок `android`:

```kotlin
    testOptions { unitTests { isReturnDefaultValues = true } }
```

- [ ] **Step 7: Проверить, что приложение всё ещё собирается**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — старый `Color.kt` на месте, экраны не тронуты.

- [ ] **Step 8: Коммит**

```bash
git add core/ui/build.gradle.kts \
        core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppColors.kt \
        core/ui/src/main/kotlin/com/appdist/core/ui/theme/AppTheme.kt \
        core/ui/src/test/kotlin/com/appdist/core/ui/theme/
git commit -m "feat(theme): ввести токены AppColors и светлую палитру"
```

---

### Task 3: Миграция компонентов core/ui

**Files:**
- Modify: `core/ui/src/main/kotlin/com/appdist/core/ui/components/VolcanicScaffold.kt`
- Modify: `core/ui/src/main/kotlin/com/appdist/core/ui/components/VolcanicSurfaces.kt`
- Modify: `core/ui/src/main/kotlin/com/appdist/core/ui/components/BuildStatusChip.kt`

**Interfaces:**
- Consumes: `AppTheme.colors` из Task 2.
- Produces: те же публичные сигнатуры компонентов (`VolcanicScaffold`, `VolcanicCard`, `RailAction`, `RailActionTile`, `GhostNumeral`, `HandednessPreview`, `BuildStatusChip` и прочие) — вызывающий код не меняется.

- [ ] **Step 1: Перевести VolcanicScaffold на токены**

В `VolcanicScaffold.kt` удалить все импорты `com.appdist.core.ui.theme.Volcanic*`, добавить `import com.appdist.core.ui.theme.AppTheme`, заменить обращения по общему правилу.

Особый случай — CTA-плитка (около строки 289). Было:

```kotlin
        color = if (enabled) VolcanicInk else VolcanicRailTarget,
        contentColor = if (enabled) VolcanicAction else VolcanicInk.copy(alpha = .45f),
```

Стало:

```kotlin
        color = if (enabled) AppTheme.colors.actionTile else AppTheme.colors.railTarget,
        contentColor = if (enabled) AppTheme.colors.onActionTile else AppTheme.colors.ink.copy(alpha = .45f),
```

Особый случай — рисование графа навигации: `AppTheme.colors` нельзя читать внутри `DrawScope`. Цвета поднимаются в переменные до `Canvas`/`drawBehind`:

```kotlin
    val graphColor = AppTheme.colors.graph
    val accent = AppTheme.colors.accent
    val accentBright = AppTheme.colors.accentBright
    val nodeRelated = AppTheme.colors.nodeRelated
    val nodeOutline = AppTheme.colors.nodeOutline
    val ground = AppTheme.colors.ground
    val ink = AppTheme.colors.ink
    Canvas(...) {
        drawPath(base, graphColor, style = graphStroke(2.dp.toPx()))
        drawPath(active, accent.copy(alpha = .6f), style = graphStroke(2.5.dp.toPx()))
        drawCircle(accent, 3.dp.toPx(), p(NodeJunction))
        // ...узлы: accent/accentBright, nodeRelated/accent, ground/nodeOutline
    }
```

То же самое в `GraphMark` (около строки 457): поднять `ink`, `accent`, `nodeRelated` перед `Canvas`.

Строка 125 — `CompositionLocalProvider(LocalContentColor provides VolcanicInk)` — становится `LocalContentColor provides AppTheme.colors.ink`.

- [ ] **Step 2: Проверить компиляцию модуля**

Run: `./gradlew :core:ui:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Перевести VolcanicSurfaces на токены**

Аналогично по общему правилу. Особый случай — `HandednessPreview` (строка ~217): фон меняется с `field` на `previewGround`.

Было:

```kotlin
            .background(VolcanicField)
            .border(1.dp, VolcanicPanelStroke, shape),
```

Стало:

```kotlin
            .background(AppTheme.colors.previewGround)
            .border(1.dp, AppTheme.colors.panelStroke, shape),
```

Полоса зоны остаётся на `AppTheme.colors.actionPreview`.

- [ ] **Step 4: Перевести BuildStatusChip на токены**

Заменить импорты `com.appdist.core.ui.theme.Status*` на `com.appdist.core.ui.theme.AppTheme` и переписать `when`:

```kotlin
    val colors = AppTheme.colors
    val (bg, fg) = when (type) {
        BuildChipType.MANDATORY -> colors.statusMandatoryContainer to colors.statusMandatory
        BuildChipType.LATEST -> colors.statusLatestContainer to colors.statusLatest
        BuildChipType.DEPRECATED -> colors.statusDeprecatedContainer to colors.statusDeprecated
        BuildChipType.EXPIRING -> colors.statusExpiringContainer to colors.statusExpiring
        BuildChipType.CHANNEL, BuildChipType.ENVIRONMENT ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
```

- [ ] **Step 5: Проверить, что модуль собран и в нём не осталось старых ссылок**

Run: `./gradlew :core:ui:compileDebugKotlin && grep -rn 'Volcanic[A-Z]' core/ui/src/main/kotlin/com/appdist/core/ui/components/ | grep -v 'fun Volcanic\|VolcanicScaffold(\|VolcanicCard(' | wc -l`
Expected: BUILD SUCCESSFUL и `0` в конце вывода (имена самих компонентов при этом сохраняются).

- [ ] **Step 6: Коммит**

```bash
git add core/ui/src/main/kotlin/com/appdist/core/ui/components/
git commit -m "refactor(theme): перевести компоненты core/ui на токены темы"
```

---

### Task 4: Миграция экранов browse

**Files:**
- Modify: `feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/projects/ProjectsScreen.kt`
- Modify: `feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/BuildsScreen.kt`
- Modify: `feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/components/BuildCard.kt`
- Modify: `feature/browse/src/main/kotlin/com/appdist/feature/browse/ui/builds/components/FilterChipsRow.kt`

**Interfaces:**
- Consumes: `AppTheme.colors` из Task 2; компоненты из Task 3 — их сигнатуры не менялись.
- Produces: ничего нового для последующих задач.

- [ ] **Step 1: Заменить токены в четырёх файлах**

Общее правило замены из шапки плана. Особый случай — `FilterPill` в `FilterChipsRow.kt` (строки ~83 и ~95): активная «таблетка» — это та же инверсия, что и CTA-плитка.

Было:

```kotlin
            .background(if (selected) VolcanicInk else VolcanicChip, shape)
            .border(1.dp, VolcanicPanelStroke, shape)
```

и ниже:

```kotlin
            color = if (selected) VolcanicAction else VolcanicInk,
```

Стало:

```kotlin
            .background(if (selected) AppTheme.colors.actionTile else AppTheme.colors.chip, shape)
            .border(1.dp, AppTheme.colors.panelStroke, shape)
```

и:

```kotlin
            color = if (selected) AppTheme.colors.onActionTile else AppTheme.colors.ink,
```

В `BuildsScreen.kt` (строки ~137 и ~143) `VolcanicGhostLabel` → `AppTheme.colors.ghostLabel`.

- [ ] **Step 2: Проверить компиляцию и отсутствие старых ссылок**

Run: `./gradlew :feature:browse:compileDebugKotlin && grep -rn 'Volcanic[A-Z]' feature/browse/src/main | grep -v 'VolcanicScaffold\|VolcanicCard' | wc -l`
Expected: BUILD SUCCESSFUL и `0`.

- [ ] **Step 3: Коммит**

```bash
git add feature/browse/src/main
git commit -m "refactor(theme): перевести экраны browse на токены темы"
```

---

### Task 5: Миграция экранов build-detail, mine и settings

**Files:**
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/BuildDetailScreen.kt`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/components/TechDetailsSection.kt`
- Modify: `feature/build-detail/src/main/kotlin/com/appdist/feature/builddetail/ui/components/ChangelogSection.kt`
- Modify: `feature/mine/src/main/kotlin/com/appdist/feature/mine/ui/MineScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/SettingsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/PermissionsScreen.kt`

**Interfaces:**
- Consumes: `AppTheme.colors` из Task 2, компоненты из Task 3.
- Produces: ничего нового.

- [ ] **Step 1: Заменить токены в файлах build-detail**

Общее правило замены. Особых случаев нет — проверить только, что цвета, используемые внутри `drawBehind`/`Canvas` (если встретятся), подняты в переменные до блока рисования, как в Task 3.

- [ ] **Step 2: Проверить компиляцию build-detail**

Run: `./gradlew :feature:build-detail:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Заменить токены в MineScreen и экранах settings**

Общее правило замены. В `SettingsScreen.kt` затрагиваются `VolcanicPanel`, `VolcanicMuted`, `VolcanicMutedDim`, `VolcanicAccent`, `VolcanicGround`, `VolcanicPanelStroke`.

- [ ] **Step 4: Проверить компиляцию и отсутствие старых ссылок во всех трёх модулях**

Run: `./gradlew :feature:build-detail:compileDebugKotlin :feature:mine:compileDebugKotlin :feature:settings:compileDebugKotlin && grep -rn 'Volcanic[A-Z]' feature/build-detail/src/main feature/mine/src/main feature/settings/src/main | grep -v 'VolcanicScaffold\|VolcanicCard' | wc -l`
Expected: BUILD SUCCESSFUL и `0`.

- [ ] **Step 5: Коммит**

```bash
git add feature/build-detail/src/main feature/mine/src/main feature/settings/src/main
git commit -m "refactor(theme): перевести экраны сборки, загрузок и настроек на токены"
```

---

### Task 6: Удаление старой палитры

**Files:**
- Delete: `core/ui/src/main/kotlin/com/appdist/core/ui/theme/Color.kt`

**Interfaces:**
- Consumes: результат Task 3-5 — прямых ссылок на `Volcanic*`/`App*`/`Status*` больше нет.
- Produces: гарантию, что новых обращений к старым константам не появится (их просто не существует).

- [ ] **Step 1: Убедиться, что на файл никто не ссылается**

Run: `grep -rn 'Volcanic[A-Z]\|StatusMandatory\|StatusLatest\|StatusDeprecated\|StatusExpiring\|AppPrimary\|AppDark\|AppBackground\|AppSurface\|AppOnSurface\|AppOutline' --include='*.kt' app core feature | grep -vE '/theme/(Color|AppColors)\.kt' | grep -vE 'VolcanicScaffold\(|VolcanicCard\(|fun VolcanicScaffold|fun VolcanicCard' | wc -l`
Expected: `0`. Фильтр исключает сам удаляемый `Color.kt` (его собственные определения к делу не относятся) и имена компонентов `VolcanicScaffold`/`VolcanicCard`, которые сохраняются намеренно.

- [ ] **Step 2: Удалить файл**

```bash
git rm core/ui/src/main/kotlin/com/appdist/core/ui/theme/Color.kt
```

- [ ] **Step 3: Собрать проект и прогнать тесты**

Run: `./gradlew assembleDebug :core:ui:testDebugUnitTest :core:datastore:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, все тесты проходят.

- [ ] **Step 4: Коммит**

```bash
git commit -m "refactor(theme): удалить плоскую палитру Color.kt"
```

---

### Task 7: Применение выбранной темы в MainActivity

**Files:**
- Create: `app/src/main/kotlin/com/appdist/app/MainViewModel.kt`
- Modify: `app/src/main/kotlin/com/appdist/app/MainActivity.kt`

**Interfaces:**
- Consumes: `ThemeMode`, `UserPreferencesStore.themeMode` из Task 1; `AppTheme`, `volcanicDarkColors()`, `volcanicLightColors()` из Task 2.
- Produces: `@HiltViewModel class MainViewModel` с `val themeMode: StateFlow<ThemeMode?>` (`null` — значение ещё не прочитано).

- [ ] **Step 1: Создать MainViewModel**

Создать `app/src/main/kotlin/com/appdist/app/MainViewModel.kt`:

```kotlin
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
```

- [ ] **Step 2: Применить тему и системные бары в MainActivity**

Заменить содержимое `app/src/main/kotlin/com/appdist/app/MainActivity.kt`:

```kotlin
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
```

- [ ] **Step 3: Собрать и проверить на устройстве**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Установить сборку и убедиться: приложение открывается в тёмной теме на телефоне с тёмной системной темой и в светлой — со светлой; иконки статус-бара в светлой теме тёмные и читаемые.

- [ ] **Step 4: Коммит**

```bash
git add app/src/main/kotlin/com/appdist/app/MainViewModel.kt \
        app/src/main/kotlin/com/appdist/app/MainActivity.kt
git commit -m "feat(theme): применять выбранный режим оформления при запуске"
```

---

### Task 8: Выбор темы в настройках

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/SettingsViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/com/appdist/feature/settings/ui/SettingsScreen.kt`
- Test: `feature/settings/src/test/kotlin/com/appdist/feature/settings/SettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `ThemeMode`, `UserPreferencesStore.themeMode`, `setThemeMode()` из Task 1; `AppTheme.colors` из Task 2.
- Produces: `SettingsUiState` с полем `themeMode: ThemeMode = ThemeMode.SYSTEM`; `SettingsViewModel.setThemeMode(mode: ThemeMode)`.

- [ ] **Step 1: Написать падающий тест на ViewModel**

Создать `feature/settings/src/test/kotlin/com/appdist/feature/settings/SettingsViewModelTest.kt`:

```kotlin
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
    fun `состояние отдаёт сохранённый режим темы`() = runTest {
        every { prefsStore.themeMode } returns flowOf(ThemeMode.LIGHT)
        val viewModel = SettingsViewModel(prefsStore)
        viewModel.state.test {
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode пишет выбор в хранилище`() = runTest {
        every { prefsStore.themeMode } returns flowOf(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(prefsStore)
        viewModel.setThemeMode(ThemeMode.DARK)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { prefsStore.setThemeMode(ThemeMode.DARK) }
    }
}
```

- [ ] **Step 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew :feature:settings:testDebugUnitTest`
Expected: FAIL — `Unresolved reference: themeMode` в `SettingsUiState`.

- [ ] **Step 3: Добавить режим темы в SettingsViewModel**

В `SettingsViewModel.kt` добавить импорты `com.appdist.core.common.model.ThemeMode` и `kotlinx.coroutines.flow.combine`, расширить состояние и добавить сеттер:

```kotlin
data class SettingsUiState(
    val actionZoneOnLeft: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
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
```

- [ ] **Step 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew :feature:settings:testDebugUnitTest`
Expected: PASS, 2 теста.

- [ ] **Step 5: Добавить блок выбора темы на экран настроек**

В `SettingsScreen.kt` добавить импорт `com.appdist.core.common.model.ThemeMode` и приватный компонент по образцу `PlacementChoice` — с той же геометрией (высота 78.dp, скругление 18.dp, точка-радио 16.dp, превью 34×52 dp справа):

```kotlin
/**
 * Выбор темы повторяет язык выбора хвата: точка, подпись и мини-превью
 * телефона, залитого фоном соответствующей темы.
 */
@Composable
private fun ThemeChoice(
    label: String,
    hint: String,
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(colors.panel, shape)
            .border(1.dp, if (selected) colors.accent else colors.panelStroke, shape)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(if (selected) colors.accent else colors.panelStroke, CircleShape)
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.mutedDim)
        }
        ThemeSwatch(mode)
    }
}

/** Мини-превью: сплошная заливка для светлой и тёмной, диагональ — для системной. */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val shape = RoundedCornerShape(10.dp)
    val dark = volcanicDarkColors().ground
    val light = volcanicLightColors().ground
    val stroke = AppTheme.colors.panelStroke
    Box(
        Modifier
            .width(34.dp)
            .height(52.dp)
            .clip(shape)
            .border(1.dp, stroke, shape)
    ) {
        when (mode) {
            ThemeMode.LIGHT -> Box(Modifier.fillMaxSize().background(light))
            ThemeMode.DARK -> Box(Modifier.fillMaxSize().background(dark))
            ThemeMode.SYSTEM -> {
                Box(Modifier.fillMaxSize().background(light))
                Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, dark)
                }
            }
        }
    }
}
```

Понадобятся импорты: `androidx.compose.foundation.Canvas`, `androidx.compose.ui.graphics.Path`, `androidx.compose.ui.draw.clip`, `androidx.compose.foundation.layout.width`, `com.appdist.core.ui.theme.volcanicDarkColors`, `com.appdist.core.ui.theme.volcanicLightColors`.

Вставить блок в `Column` экрана — выше заголовка «Положение action-зоны»:

```kotlin
            Spacer(Modifier.height(24.dp))
            Text(
                "Тема",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.muted,
            )
            Spacer(Modifier.height(14.dp))
            ThemeChoice(
                label = "Как в системе",
                hint = "следовать настройке телефона",
                mode = ThemeMode.SYSTEM,
                selected = state.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            Spacer(Modifier.height(12.dp))
            ThemeChoice(
                label = "Светлая",
                hint = "минеральная поверхность",
                mode = ThemeMode.LIGHT,
                selected = state.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            Spacer(Modifier.height(12.dp))
            ThemeChoice(
                label = "Тёмная",
                hint = "базальтовая земля",
                mode = ThemeMode.DARK,
                selected = state.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
```

- [ ] **Step 6: Собрать и проверить переключение вживую**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

На устройстве: открыть настройки, переключить «Светлая» — приложение перекрашивается сразу, без перезапуска; иконки статус-бара становятся тёмными; вернуться на «Как в системе» — тема следует системной.

- [ ] **Step 7: Коммит**

```bash
git add feature/settings/src/main feature/settings/src/test
git commit -m "feat(settings): добавить выбор темы оформления"
```

---

### Task 9: Превью компонентов и проверка экранов вне макета

**Files:**
- Create: `core/ui/src/main/kotlin/com/appdist/core/ui/components/ThemePreviews.kt`
- Modify (при необходимости): `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginScreen.kt`, `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpScreen.kt`, `feature/upload/src/main/kotlin/com/appdist/feature/upload/ui/UploadScreen.kt`, `feature/home/src/main/kotlin/com/appdist/feature/home/ui/HomeScreen.kt`, `core/ui/src/main/kotlin/com/appdist/core/ui/components/ErrorScreen.kt`, `core/ui/src/main/kotlin/com/appdist/core/ui/components/EmptyScreen.kt`, `core/ui/src/main/kotlin/com/appdist/core/ui/components/LoadingScreen.kt`

**Interfaces:**
- Consumes: всё, что сделано в Task 2-8.
- Produces: превью для визуальной сверки; правки экранов вне макета.

- [ ] **Step 1: Добавить превью в двух темах**

Создать `core/ui/src/main/kotlin/com/appdist/core/ui/components/ThemePreviews.kt`:

```kotlin
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

@Preview(name = "Светлая", showBackground = true)
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
```

Если сигнатура `VolcanicCard` требует иных параметров — подставить их по фактическому объявлению в `VolcanicSurfaces.kt`.

- [ ] **Step 2: Проверить сборку превью**

Run: `./gradlew :core:ui:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Открыть `ThemePreviews.kt` в Android Studio и убедиться, что оба превью рисуются и светлое соответствует макету.

- [ ] **Step 3: Пройти экраны вне макета в светлой теме**

Собрать и установить сборку, переключить тему на светлую и открыть: Login, OTP, Upload, Home, Permissions, а также состояния загрузки, пустого списка и ошибки.

Искать: белый текст на белом фоне, невидимые иконки, тёмные плашки посреди светлого экрана. Править точечно, заменяя жёсткие цвета на `AppTheme.colors.*` из токен-таблицы; новых цветов не вводить.

- [ ] **Step 4: Финальная проверка**

Run: `./gradlew assembleDebug test`
Expected: BUILD SUCCESSFUL, все тесты проходят.

Пройти чеклист приёмки из спеки:
1. Семь экранов из макета в светлой теме совпадают с `Volcanic Split Flow · Light Theme`.
2. Экраны вне макета читаемы.
3. Переключение темы работает на лету, иконки баров переворачиваются, экран не перезапускается.
4. Холодный старт в каждом из трёх режимов — без вспышки чужого фона.
5. Тёмная тема не отличается от сборки до изменений.

- [ ] **Step 5: Коммит**

```bash
git add core/ui/src/main/kotlin/com/appdist/core/ui/components/ThemePreviews.kt
# добавлять точечно только те экраны, которые правились на шаге 3 —
# в рабочем дереве есть посторонние незакоммиченные изменения, `git add -u` их захватит
git add feature/auth/src/main feature/upload/src/main feature/home/src/main core/ui/src/main
git commit -m "feat(theme): превью компонентов в двух темах и правки экранов вне макета"
```
