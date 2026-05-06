package com.vboard.aac.di

import android.content.Context
import androidx.room.Room
import com.vboard.aac.data.local.db.VBoardDatabase
import com.vboard.aac.data.local.db.dao.CategoryDao
import com.vboard.aac.data.local.db.dao.StatsDao
import com.vboard.aac.data.local.db.dao.VocabCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VBoardDatabase {
        return Room.databaseBuilder(
            context,
            VBoardDatabase::class.java,
            VBoardDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideVocabCardDao(database: VBoardDatabase): VocabCardDao {
        return database.vocabCardDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: VBoardDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideStatsDao(database: VBoardDatabase): StatsDao {
        return database.statsDao()
    }
}
