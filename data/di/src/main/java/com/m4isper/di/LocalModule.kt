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
        )
            // Wiping is the right call for this cache: every row is derived from the
            // feed, and `refresh()` rebuilds page 1, so a schema bump costs the user
            // their scroll position and nothing else. The alternative — no migration
            // path at all — turns the next bump into a crash on launch.
            //
            // It is a decision, not a default, so it stays written down here. The
            // schemas that would make a real `Migration` possible are committed in
            // `data/local/schemas`.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideApodDao(database: PlanetsDatabase): ApodDao = database.apodDao()
}
