package com.vboard.aac.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vboard.aac.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vboard_settings")

@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val VOICE_TYPE = stringPreferencesKey("voice_type")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkMode = prefs[Keys.DARK_MODE] ?: false,
            gridColumns = prefs[Keys.GRID_COLUMNS] ?: 3,
            showLabels = prefs[Keys.SHOW_LABELS] ?: true,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            pinCode = prefs[Keys.PIN_CODE] ?: "1234",
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 1.0f,
            voiceType = prefs[Keys.VOICE_TYPE] ?: "nam-bac"
        )
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.GRID_COLUMNS] ?: 3
    }

    val showLabels: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_LABELS] ?: true
    }

    val fontScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_SCALE] ?: 1.0f
    }

    val pinCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.PIN_CODE] ?: "1234"
    }

    val voiceVolume: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.VOICE_VOLUME] ?: 1.0f
    }

    val voiceType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VOICE_TYPE] ?: "nam-bac"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GRID_COLUMNS] = columns.coerceIn(2, 4)
        }
    }

    suspend fun setShowLabels(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_LABELS] = show
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale.coerceIn(0.75f, 1.5f)
        }
    }

    suspend fun setPinCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_CODE] = code
        }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOICE_VOLUME] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun setVoiceType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOICE_TYPE] = type
        }
    }

    suspend fun isOnboardingCompleted(): Boolean {
        var completed = false
        context.dataStore.edit { prefs ->
            completed = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        }
        return completed
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
