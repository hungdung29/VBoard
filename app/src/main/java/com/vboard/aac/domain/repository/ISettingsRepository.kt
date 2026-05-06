package com.vboard.aac.domain.repository

import com.vboard.aac.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    val darkMode: Flow<Boolean>
    val gridColumns: Flow<Int>
    val showLabels: Flow<Boolean>
    val fontScale: Flow<Float>
    val pinCode: Flow<String>
    val voiceVolume: Flow<Float>
    val voiceType: Flow<String>
    val appSettings: Flow<AppSettings>

    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setGridColumns(columns: Int)
    suspend fun setShowLabels(show: Boolean)
    suspend fun setFontScale(scale: Float)
    suspend fun setPinCode(code: String)
    suspend fun setVoiceVolume(volume: Float)
    suspend fun setVoiceType(type: String)
    suspend fun verifyPin(pin: String): Boolean
}
