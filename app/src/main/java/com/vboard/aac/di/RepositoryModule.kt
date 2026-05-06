package com.vboard.aac.di

import com.vboard.aac.data.repository.SettingsRepositoryImpl
import com.vboard.aac.data.repository.StatsRepositoryImpl
import com.vboard.aac.data.repository.VocabRepositoryImpl
import com.vboard.aac.domain.repository.ISettingsRepository
import com.vboard.aac.domain.repository.IStatsRepository
import com.vboard.aac.domain.repository.IVocabRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVocabRepository(impl: VocabRepositoryImpl): IVocabRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): ISettingsRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): IStatsRepository
}
