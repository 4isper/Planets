package com.m4isper.planets.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The date is a constructor argument rather than something read from SavedStateHandle.
 *
 * Navigation 3 hands the NavKey to the destination lambda but does not copy its fields
 * into SavedStateHandle, which is what Navigation 2 used to do via navArgument.
 */
class PlanetDetailViewModel(
    private val date: String,
    private val getPlanetDetails: GetPlanetDetailsUseCase,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = getPlanetDetails(date)
        .map { planet ->
            if (planet == null) DetailUiState.Empty else DetailUiState.Content(planet.toUi())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading,
        )
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Empty : DetailUiState
    data class Content(val planet: PlanetUi) : DetailUiState
}