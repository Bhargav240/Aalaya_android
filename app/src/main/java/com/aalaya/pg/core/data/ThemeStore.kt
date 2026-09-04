package com.aalaya.pg.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hamsaThemeStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    by preferencesDataStore(name = "hamsa_theme")

/** Light/dark follows the OS by default; either can be pinned instead. */
enum class ThemeMode(val wire: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromWire(s: String?): ThemeMode = entries.firstOrNull { it.wire == s } ?: System
    }
}

/**
 * The user's appearance preference. Device-local only -- there's no backend
 * concept of theme, so this never touches the network, just like [PinStore].
 */
@Singleton
class ThemeStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val modeKey = stringPreferencesKey("mode")

    val mode: Flow<ThemeMode> = context.hamsaThemeStore.data.map { ThemeMode.fromWire(it[modeKey]) }

    suspend fun setMode(mode: ThemeMode) {
        context.hamsaThemeStore.edit { it[modeKey] = mode.wire }
    }
}
