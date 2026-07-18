package dev.study.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "study_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
        private val KEY_PRESET = stringPreferencesKey("color_preset") // OCEAN, FOREST, SUNSET, MONOCHROME, SLATE
        private val KEY_FONT = stringPreferencesKey("font_family") // DEFAULT, INTER, OUTFIT, ROBOTO
        private val KEY_DYNAMIC_COLOR = stringPreferencesKey("dynamic_color") // ENABLED, DISABLED
        private val KEY_DND_FILTER = stringPreferencesKey("dnd_filter") // PRIORITY, SILENCE
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    val colorPresetFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRESET] ?: "OCEAN"
    }

    val fontFamilyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT] ?: "DEFAULT"
    }

    val dynamicColorFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: "DISABLED"
    }

    val dndFilterFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DND_FILTER] ?: "PRIORITY"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setColorPreset(preset: String) {
        context.dataStore.edit { it[KEY_PRESET] = preset }
    }

    suspend fun setFontFamily(font: String) {
        context.dataStore.edit { it[KEY_FONT] = font }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = if (enabled) "ENABLED" else "DISABLED" }
    }

    suspend fun setDndFilter(filter: String) {
        context.dataStore.edit { it[KEY_DND_FILTER] = filter }
    }
}
