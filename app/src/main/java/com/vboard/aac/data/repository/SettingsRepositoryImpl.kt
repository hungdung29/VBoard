package com.vboard.aac.data.repository

import com.vboard.aac.data.local.datastore.AppPreferencesDataStore
import com.vboard.aac.domain.model.AppSettings
import com.vboard.aac.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: AppPreferencesDataStore
) : ISettingsRepository {

    override val darkMode: Flow<Boolean> = dataStore.darkMode

    override val gridColumns: Flow<Int> = dataStore.gridColumns

    override val showLabels: Flow<Boolean> = dataStore.showLabels

    override val fontScale: Flow<Float> = dataStore.fontScale

    override val pinCode: Flow<String> = dataStore.pinCode

    override val voiceVolume: Flow<Float> = dataStore.voiceVolume

    override val voiceType: Flow<String> = dataStore.voiceType

    override val voiceCloningEnabled: Flow<Boolean> = dataStore.voiceCloningEnabled

    override val vieneuServerUrl: Flow<String> = dataStore.vieneuServerUrl

    override val appSettings: Flow<AppSettings> = dataStore.appSettings

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.setDarkMode(enabled)
    }

    override suspend fun setGridColumns(columns: Int) {
        dataStore.setGridColumns(columns)
    }

    override suspend fun setShowLabels(show: Boolean) {
        dataStore.setShowLabels(show)
    }

    override suspend fun setFontScale(scale: Float) {
        dataStore.setFontScale(scale)
    }

    override suspend fun setPinCode(code: String) {
        dataStore.setPinCode(code)
    }

    override suspend fun setVoiceVolume(volume: Float) {
        dataStore.setVoiceVolume(volume)
    }

    override suspend fun setVoiceType(type: String) {
        dataStore.setVoiceType(type)
    }

    override suspend fun setVoiceCloningEnabled(enabled: Boolean) {
        dataStore.setVoiceCloningEnabled(enabled)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return pin == dataStore.pinCode.first()
    }
}
