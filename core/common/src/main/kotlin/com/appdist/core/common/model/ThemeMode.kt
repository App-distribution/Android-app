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
