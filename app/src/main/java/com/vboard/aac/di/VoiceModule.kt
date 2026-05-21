package com.vboard.aac.di

import android.content.Context
import com.vboard.aac.data.repository.VoiceProfileRepositoryImpl
import com.vboard.aac.domain.repository.IVoiceProfileRepository
import com.vboard.aac.platform.audio.AudioQualityAnalyzer
import com.vboard.aac.platform.audio.AudioRecorderManager
import com.vboard.aac.platform.tts.ValtecTtsEngine
import com.vboard.aac.platform.voice.DeviceCapabilityDetector
import com.vboard.aac.platform.voice.VoiceProfileManager
import com.vboard.aac.platform.voice.VoiceRecordingManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVoiceProfileRepository(
        impl: VoiceProfileRepositoryImpl
    ): IVoiceProfileRepository
}

@Module
@InstallIn(SingletonComponent::class)
object VoicePlatformModule {

    @Provides
    @Singleton
    fun provideAudioRecorderManager(
        @ApplicationContext context: Context
    ): AudioRecorderManager = AudioRecorderManager(context)

    @Provides
    @Singleton
    fun provideAudioQualityAnalyzer(): AudioQualityAnalyzer = AudioQualityAnalyzer()

    @Provides
    @Singleton
    fun provideDeviceCapabilityDetector(
        @ApplicationContext context: Context
    ): DeviceCapabilityDetector = DeviceCapabilityDetector(context)

    @Provides
    @Singleton
    fun provideValtecTtsEngine(
        @ApplicationContext context: Context
    ): ValtecTtsEngine = ValtecTtsEngine(context)

    @Provides
    @Singleton
    fun provideVoiceRecordingManager(
        @ApplicationContext context: Context
    ): VoiceRecordingManager = VoiceRecordingManager(context)

    @Provides
    @Singleton
    fun provideVoiceProfileManager(
        voiceRecordingManager: VoiceRecordingManager,
        valtecTtsEngine: ValtecTtsEngine,
        voiceProfileRepository: IVoiceProfileRepository,
        audioQualityAnalyzer: AudioQualityAnalyzer
    ): VoiceProfileManager = VoiceProfileManager(
        voiceRecordingManager,
        valtecTtsEngine,
        voiceProfileRepository,
        audioQualityAnalyzer
    )
}
