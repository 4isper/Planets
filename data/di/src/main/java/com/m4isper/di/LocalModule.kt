package com.m4isper.di

import android.content.Context
import androidx.room.Room
import com.m4isper.local.PlanetsDatabase
import com.m4isper.local.dao.ApodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlanetsDatabase =
        Room.databaseBuilder(
            context,
            PlanetsDatabase::class.java,
            "planets.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideApodDao(database: PlanetsDatabase): ApodDao = database.apodDao()
}
