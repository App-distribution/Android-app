# Светлая тема Volcanic Split

Дата: 2026-08-18
Статус: согласовано, готово к планированию реализации

## Задача

Приложение существует только в тёмном виде. Нужна светлая тема по макету
`Volcanic Split Flow · Light Theme` (Pencil, `untitled.pen`, узел `AwXPi`) и
возможность выбрать её в настройках.

## Исходное состояние

- `AppTheme.kt` объявляет `LightColorScheme`, но собирает её из тех же
  `Volcanic*` констант, что и тёмную: `AppBackground = VolcanicGround`
  (`#080B0C`). Светлой темы фактически нет.
- `MainActivity` вызывает `AppTheme(darkTheme = true)` — системная настройка не
  читается вообще.
- Цвета живут как top-level `val` в `core/ui/theme/Color.kt`. Экраны обращаются
  к ним напрямую: 202 обращения к 26 токенам в 12 файлах. Ни один из них не
  переключается по теме в принципе.
- `MaterialTheme.colorScheme` используется лишь в 19 местах.
- Скриншот-тестов нет (ни Paparazzi, ни Roborazzi), `@Preview` в проекте нет.
  Тесты — JUnit5 + Turbine + coroutines-test на JVM.

Суть работы — не подбор палитры (она задана макетом), а превращение плоских
констант в токены с двумя значениями.

## Решения

### Выбор темы

Три режима: «Как в системе» (по умолчанию), «Светлая», «Тёмная».

### Охват

Всё приложение. Экраны вне макета (Login, OTP, Upload, Home, Permissions,
Loading/Empty/Error) выводятся из тех же токенов — новых цветов для них не
вводится.

### Архитектура цвета

Источник истины — `AppColors`, из него выводится `MaterialTheme.colorScheme`.

Рассмотренные альтернативы:

- **Только `colorScheme`.** У Material 3 нет слотов под граф навигации, ghost
  numeral, панель со stroke `0x55FFFFFF`, rail target. Пришлось бы занимать
  `surfaceTint`/`tertiary` под чужую семантику, и часть токенов всё равно
  осталась бы снаружи.
- **Только `AppColors`, `colorScheme` не трогаем.** M3-компоненты (`Text` без
  явного цвета, `OutlinedButton`, `TextField`, ripple) продолжили бы брать цвета
  из фиктивной схемы и давали бы тёмные пятна в светлой теме.
- **Принято: `AppColors` + выведенная из него `colorScheme`.** Экраны читают
  фирменные токены, M3-дефолты остаются корректными в обеих темах, 19 мест с
  `MaterialTheme.colorScheme` переписывать не нужно.

## Слой токенов

Новый файл `core/ui/theme/AppColors.kt`:

```kotlin
@Immutable
data class AppColors(
    // поверхности
    val ground: Color, val surface: Color, val surfaceRaised: Color,
    val panel: Color, val panelStroke: Color, val panelStrokeStrong: Color,
    val field: Color, val chip: Color, val track: Color, val outline: Color,
    // текст
    val ink: Color, val inkSoft: Color, val muted: Color, val mutedDim: Color,
    val ghost: Color, val ghostLabel: Color,
    // акцент и действие
    val accent: Color, val accentBright: Color, val action: Color,
    val actionTile: Color, val onActionTile: Color,
    // рельс и граф навигации
    val railTarget: Color, val railTargetStroke: Color, val railDivider: Color,
    val graph: Color, val nodeRelated: Color, val nodeOutline: Color,
    // превью хвата
    val previewGround: Color, val actionPreview: Color,
    // статусы
    val statusMandatory: Color, val statusMandatoryContainer: Color,
    val statusDeprecated: Color, val statusDeprecatedContainer: Color,
    val statusExpiring: Color, val statusExpiringContainer: Color,
    val statusLatest: Color, val statusLatestContainer: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("AppTheme не подключён") }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
```

Две фабрики — `volcanicDarkColors()` и `volcanicLightColors()`. Композабл
`AppTheme(darkTheme, content)` кладёт выбранную палитру в `LocalAppColors` и
собирает из неё `colorScheme` (`background = ground`, `surface = surface`,
`onSurface = ink`, `onSurfaceVariant = muted`, `primary = accent`,
`outline = outline` и далее по ролям).

### Палитра

Тёмная колонка переносится байт в байт из нынешнего `Color.kt`. Светлая взята
из макета; строки, помеченные «выведено», в макете отсутствуют и обоснованы
отдельно ниже.

| Токен | Dark | Light | Источник |
|---|---|---|---|
| `ground` | `#080B0C` | `#F8FAF7` | фон экранов |
| `surface` | `#111718` | `#FFFFFF` | Search Field, Build Row |
| `surfaceRaised` | `#182123` | `#EFF5F2` | Right Choice Preview |
| `panel` | `0xCC10191A` | `0xF2FFFFFF` | Project Log Entry |
| `panelStroke` | `0x55FFFFFF` | `#AFC3BD` | stroke карточек |
| `panelStrokeStrong` | `0x66FFFFFF` | `#9DB2AC` | Build Row stroke |
| `field` | `0x99050808` | `#FFFFFF` | Search Field |
| `chip` | `0xAA050808` | `#FFFFFF` | Channel Filters |
| `track` | `#263637` | `#C8D6D1` | Storage Track, Reach Track |
| `outline` | `#30433F` | `#AFC3BD` | разделители |
| `ink` | `#ECF1EF` | `#132D29` | заголовки |
| `inkSoft` | `#D5E0DD` | `#334D47` | вторичный текст |
| `muted` | `#A9BCB8` | `#516A65` | подписи |
| `mutedDim` | `#8FA7A3` | `#5B736D` | мелкие подписи, скорректировано |
| `ghost` | `0x08FFFFFF` | `0x0D0B3F3C` | Ghost Numeral |
| `ghostLabel` | `0x33AFC5C0` | `0x665B736D` | выведено |
| `accent` | `#9DDDD2` | `#0B756C` | teal-акцент |
| `accentBright` | `#C9FFF5` | `#F4FFFC` | обводка активного узла |
| `action` | `#063F47` | `#D6ECE7` | Teal Route Rail |
| `actionTile` | `#ECF1EF` | `#132D29` | CTA-плитка, активный фильтр |
| `onActionTile` | `#063F47` | `#FFFFFF` | текст на CTA-плитке |
| `railTarget` | `#052F35` | `#C4E0DA` | Settings Touch Target |
| `railTargetStroke` | `0x33FFFFFF` | `#C5D3CF` | его stroke |
| `railDivider` | `0x66FFFFFF` | `#9DB2AC` | Rail Divider |
| `graph` | `0x387FA09A` | `0x3F819C96` | Navigation Graph · Base |
| `nodeRelated` | `#376C67` | `#84B6AD` | Nav Node · Related |
| `nodeOutline` | `0x777FA09A` | `0x8A6F8F88` | Nav Node · Inactive stroke |
| `previewGround` | `0x99050808` | `#EFF5F2` | Right Choice Preview |
| `actionPreview` | `#0C6974` | `#0B756C` | полоса зоны в превью |

Статусные цвета (в макете чипов со статусами нет; светлые значения выведены по
принципу «тёмный foreground на светлом контейнере», сверены с переменными
дизайн-системы в том же `.pen`: `--color-error` = `#E5DCDA`/`#8C1C00`,
`--color-warning` = `#E9E3D8`/`#804200`):

| Токен | Dark | Light |
|---|---|---|
| `statusMandatory` | `#FFB4AB` | `#8C1C00` |
| `statusMandatoryContainer` | `#4A1D1B` | `#F0DEDA` |
| `statusDeprecated` | `#CAD4D1` | `#4A5754` |
| `statusDeprecatedContainer` | `#29302F` | `#E4E9E7` |
| `statusExpiring` | `#FFD18A` | `#804200` |
| `statusExpiringContainer` | `#4A3210` | `#F2E6D3` |
| `statusLatest` | `#9DDDD2` | `#0B756C` |
| `statusLatestContainer` | `#063F47` | `#D6ECE7` |

Одно отступление от макета:

- `mutedDim` в макете — `#607A74`, что на фоне `ground` даёт контраст 4.35:1
  при пороге WCAG AA 4.5:1, а набран им мелкий текст 11px. Значение затемнено
  до `#5B736D` (4.78:1); оттенок сохранён. Макет правится соответственно.

Два выведенных значения помимо статусов:

- `ghostLabel` — макет не содержит ghost-подписей в светлой версии. Берётся
  `mutedDim` с альфой `0x66`, что повторяет логику тёмной темы (приглушённый
  вариант основного muted-цвета).
- `previewGround` в тёмной теме намеренно равен нынешнему `field`
  (`0x99050808`), а не новому `surfaceRaised`. Иначе превью хвата поменяло бы
  вид в тёмной теме, а она меняться не должна.

## Выбор темы

**Модель.** `ThemeMode { SYSTEM, LIGHT, DARK }` в
`core/common/model/ThemeMode.kt`. `core/ui` уже зависит от `core:common`, а
`core/datastore` — нет, ему нужно добавить `implementation(project(":core:common"))`.

**Хранение.** В `UserPreferencesStore` добавляются
`stringPreferencesKey("theme_mode")`, `themeMode: Flow<ThemeMode>` с дефолтом
`SYSTEM` и `setThemeMode()` — тем же паттерном, что уже есть у
`actionZoneOnLeft`. Неизвестное значение в prefs трактуется как `SYSTEM`.

**Применение.** `MainViewModel` отдаёт `StateFlow<ThemeMode?>`, где `null` —
«ещё не прочитано из DataStore». Пока `null`, контент не рисуется: иначе
пользователь со светлой темой на тёмном телефоне увидит кадр тёмного экрана.
Дальше `AppTheme(darkTheme = mode.resolve(isSystemInDarkTheme()))`.

**Системные бары.** Нынешний `SystemBarStyle.dark(TRANSPARENT)` даёт белые
иконки статус-бара на белом фоне. `enableEdgeToEdge` переносится в
`LaunchedEffect(darkTheme)` и вызывается с
`SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { darkTheme }`, чтобы иконки
переворачивались вместе с темой, в том числе при переключении на лету.

**Фон окна.** `Theme.AppDistribution` наследует
`android:Theme.DeviceDefault.NoActionBar`, поэтому до первого кадра Compose фон
берётся из системной темы: при «светлая тема приложения + тёмная система» будет
тёмная вспышка. Лечится программно — после чтения настройки выставляем
`window.setBackgroundDrawable(ColorDrawable(ground))`.

**UI в настройках.** Макета для блока нет; строится по образцу существующего
`PlacementChoice` — карточка с точкой-радио, заголовком, подсказкой и мини-
превью. Три карточки: «Как в системе», «Светлая», «Тёмная». Превью — силуэт
телефона, залитый `ground` соответствующей темы; у системной — диагональное
деление светлой и тёмной половин. Блок располагается выше выбора положения
action-зоны.

## Миграция экранов

Порядок снизу вверх, каждый шаг компилируется:

1. **`core/ui/theme`** — `AppColors.kt`, переписанный `AppTheme.kt`. Старые
   top-level `val Volcanic*` становятся `private` внутри файла палитр. После
   этого любое старое обращение перестаёт компилироваться, и `./gradlew
   assembleDebug` сам находит все 202 места.
2. **`core/ui/components`** — `VolcanicScaffold.kt` (граф, рельс, CTA-плитки),
   `VolcanicSurfaces.kt` (панели, ghost, треки, превью хвата),
   `BuildStatusChip.kt`. Здесь же `LocalContentColor provides colors.ink`.
3. **Экраны из макета** — `browse` (ProjectsScreen, BuildsScreen, BuildCard,
   FilterChipsRow), `build-detail` (BuildDetailScreen, TechDetailsSection,
   ChangelogSection), `mine` (MineScreen), `settings` (SettingsScreen,
   PermissionsScreen).
4. **Экраны вне макета** — Login, OTP, Upload, Home, Loading/Empty/Error. Они
   живут на `MaterialTheme.colorScheme` и получают светлую тему автоматически;
   работа здесь — проверка и точечные правки.

Замены механические (`VolcanicGround` → `AppTheme.colors.ground`) кроме трёх
мест:

- `RailActionTile` (`VolcanicScaffold.kt`) и `FilterPill`
  (`FilterChipsRow.kt`) используют пару «фон `ink` / текст `action`» — обе
  переходят на `actionTile`/`onActionTile`.
- `HandednessPreview` (`VolcanicSurfaces.kt`) переходит с `field` на
  `previewGround`.
- `BuildStatusChip` переводится на статусные токены из `AppColors`. Компонент
  сейчас не используется ни одним feature-модулем; удаление как мёртвого кода —
  отдельное решение, в рамках этой работы он просто мигрирует.

## Верификация

**Юнит-тесты** (JUnit5, рядом с `TokenManagerTest`):

- `UserPreferencesStoreTest` — дефолт `SYSTEM`, запись и чтение `LIGHT`/`DARK`,
  откат к `SYSTEM` при неизвестном значении. Пишется до реализации.
- `AppColorsTest` — снимок тёмной палитры: каждое поле `volcanicDarkColors()`
  сверяется с литералом из нынешнего `Color.kt`. Машинная страховка обещания
  «тёмная тема не меняется».
- `LightPaletteContrastTest` — контраст по WCAG для светлой палитры:
  `ink`/`ground`, `muted`/`panel`, `mutedDim`/`ground`,
  `onActionTile`/`actionTile`, статусные пары — не ниже 4.5:1 для мелкого
  текста и 3:1 для крупного. Если `androidx.compose.ui.graphics.Color` окажется
  недоступен в JVM-тестах, тест переводится на ARGB-литералы.

**Compose Preview.** `ui-tooling-preview` и `ui-tooling` в `core/ui` уже
подключены — новых зависимостей не нужно. `@Preview` в паре
`UI_MODE_NIGHT_NO`/`UI_MODE_NIGHT_YES` добавляются для компонентов, а не
экранов (экраны завязаны на `hiltViewModel()`): рельс и CTA-плитка, карточка
проекта, `BuildCard`, `FilterChipsRow`, новый выбор темы.

**Ручной чеклист — он же критерии приёмки:**

1. Семь экранов из макета в светлой теме совпадают с
   `Volcanic Split Flow · Light Theme`.
2. Экраны вне макета (Login, OTP, Upload, Home, Permissions, состояния) в
   светлой теме читаемы, тёмных пятен нет.
3. Переключение темы в настройках перекрашивает приложение на лету, иконки
   статус-бара переворачиваются, экран не перезапускается.
4. Холодный старт в каждом из трёх режимов не даёт вспышки чужого фона.
5. Тёмная тема визуально не отличается от сборки до изменений.
6. `./gradlew assembleDebug` и все юнит-тесты проходят.
