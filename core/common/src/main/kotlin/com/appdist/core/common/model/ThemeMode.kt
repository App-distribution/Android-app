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
        /** Основной утверждённый вариант Clean Ledger — светлый. */
        val DEFAULT = LIGHT

        /** Значение из хранилища: пустое или неизвестное трактуем как режим по умолчанию. */
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DEFAULT
    }
}
