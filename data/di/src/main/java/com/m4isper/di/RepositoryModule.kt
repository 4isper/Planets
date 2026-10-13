package com.m4isper.di

import com.m4isper.domain.repository.PlanetRepository
import com.m4isper.repository.PlanetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlanetRepository(
        impl: PlanetRepositoryImpl,
    ): PlanetRepository
}
