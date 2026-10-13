package com.m4isper.planets.presentation.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.domain.usecase.LoadPlanetDetailsUseCase
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The date is a constructor argument rather than something read from SavedStateHandle.
 *
 * Navigation 3 hands the NavKey to the destination lambda but does not copy its fields
 * into SavedStateHandle, which is what Navigation 2 used to do via navArgument.
 */
class PlanetDetailViewModel(
    private val date: String,
    private val getPlanetDetails: GetPlanetDetailsUseCase,
    private val loadPlanetDetails: LoadPlanetDetailsUseCase,
) : ViewModel() {

    private val fetchError = MutableStateFlow<Throwable?>(null)
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    val uiState: StateFlow<DetailUiState> = combine(
        getPlanetDetails(date).map { it?.toUi() },
        fetchError,
    ) { planet, error ->
        when {
            planet != null -> DetailUiState.Content(planet)
            error != null -> DetailUiState.Error(error)
            else -> DetailUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    /**
     * The screen is driven by the cache, so a restored back stack can leave it with
     * nothing to show. Re-querying the cache would change nothing; fetch instead.
     */
    fun retry() {
        // Claim the flag synchronously: setting it inside the coroutine lets a
        // second call in the same frame pass the guard.
        if (_isRetrying.value) return
        _isRetrying.value = true

        viewModelScope.launch {
            runCatching { loadPlanetDetails(date) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to fetch planet $date", error)
                    fetchError.value = error
                }
            _isRetrying.value = false
        }
    }

    private companion object {
        const val TAG = "PlanetDetailViewModel"
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /** Nothing in the cache yet; the screen can still fetch the record. */
    data object Empty : DetailUiState

    data class Error(val cause: Throwable) : DetailUiState

    data class Content(val planet: PlanetUi) : DetailUiState
}
