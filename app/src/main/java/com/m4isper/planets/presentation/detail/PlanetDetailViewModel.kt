package com.m4isper.planets.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlanetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlanetDetails: GetPlanetDetailsUseCase,
) : ViewModel() {

    private val date: String = checkNotNull(savedStateHandle["date"])

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