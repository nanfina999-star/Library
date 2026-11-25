package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

interface ThemePreferencesRepository {
    val themeMode: Flow<AppThemeMode>
    val soundEffectsEnabled: Flow<Boolean>
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setSoundEffectsEnabled(enabled: Boolean)
    suspend fun toggleLightDark(currentlyDark: Boolean) {
        setThemeMode(if (currentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK)
    }
    suspend fun toggleSoundEffects(currentlyEnabled: Boolean) {
        setSoundEffectsEnabled(!currentlyEnabled)
    }
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class DataStoreThemePreferencesRepository(
    private val context: Context,
) : ThemePreferencesRepository {

    private val themeModeKey = stringPreferencesKey("app_theme_mode")
    private val soundEffectsKey = booleanPreferencesKey("ui_sound_effects_enabled")

    override val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey]
        runCatching { if (raw != null) AppThemeMode.valueOf(raw) else AppThemeMode.SYSTEM }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    override val soundEffectsEnabled: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[soundEffectsKey] ?: true
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    override suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[soundEffectsKey] = enabled
        }
    }
}

class InMemoryThemePreferencesRepository(
    initialMode: AppThemeMode = AppThemeMode.SYSTEM,
    initialSoundEnabled: Boolean = true,
) : ThemePreferencesRepository {
    private val _themeMode = MutableStateFlow(initialMode)
    override val themeMode: Flow<AppThemeMode> = _themeMode

    private val _soundEffectsEnabled = MutableStateFlow(initialSoundEnabled)
    override val soundEffectsEnabled: Flow<Boolean> = _soundEffectsEnabled

    override suspend fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    override suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        _soundEffectsEnabled.value = enabled
    }
}
