package com.m4isper.planets.presentation.detail

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.domain.usecase.LoadPlanetDetailsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Bridge to the use cases for the detail ViewModel.
 *
 * Navigation 3 passes the destination key to the screen, so the ViewModel is created
 * with that argument through a plain factory instead of the parameterless
 * `hiltViewModel()` + SavedStateHandle route, which Navigation 3 no longer feeds.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlanetDetailEntryPoint {
    fun getPlanetDetails(): GetPlanetDetailsUseCase

    fun loadPlanetDetails(): LoadPlanetDetailsUseCase
}

internal fun planetDetailViewModelFactory(
    applicationContext: android.content.Context,
    date: String,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PlanetDetailEntryPoint::class.java,
        )
        PlanetDetailViewModel(
            date = date,
            getPlanetDetails = entryPoint.getPlanetDetails(),
            loadPlanetDetails = entryPoint.loadPlanetDetails(),
        )
    }
}