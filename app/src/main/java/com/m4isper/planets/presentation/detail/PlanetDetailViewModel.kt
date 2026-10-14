package com.m4isper.planets.presentation.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.domain.usecase.LoadPlanetDetailsResult
import com.m4isper.domain.usecase.LoadPlanetDetailsUseCase
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The date is a constructor argument rather than something read from SavedStateHandle.
 *
 * Navigation 3 hands the NavKey to the destination lambda but does not copy its fields
 * into SavedStateHandle, which is what Navigation 2 used to do via navArgument.
 *
 * @param mappingDispatcher runs the Jsoup rich-text mapping: CPU-bound work that
 * must not ride the Main thread. Injected (rather than hardcoded) so tests can
 * supply a test dispatcher and stay deterministic.
 */
class PlanetDetailViewModel(
    private val date: String,
    private val getPlanetDetails: GetPlanetDetailsUseCase,
    private val loadPlanetDetails: LoadPlanetDetailsUseCase,
    private val mappingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    /** What the last fetch settled on. `null` means nothing has been attempted yet. */
    private val fetchOutcome = MutableStateFlow<FetchOutcome?>(null)
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    val uiState: StateFlow<DetailUiState> = combine(
        getPlanetDetails(date).map { it?.toUi() }.flowOn(mappingDispatcher),
        fetchOutcome,
    ) { planet, outcome ->
        when {
            // The cache wins: a successful fetch needs no state of its own, the
            // Room flow re-emits on its own once the record lands.
            planet != null -> DetailUiState.Content(planet)
            outcome is FetchOutcome.Failed -> DetailUiState.Error(outcome.cause)
            outcome is FetchOutcome.NotFound -> DetailUiState.NotFound
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
            try {
                fetchOutcome.value = when (loadPlanetDetails(date)) {
                    // Clearing the outcome is what un-sticks a screen that failed
                    // once and succeeded on a later attempt.
                    LoadPlanetDetailsResult.Fetched -> null
                    LoadPlanetDetailsResult.NotFound -> FetchOutcome.NotFound
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to fetch planet $date", error)
                fetchOutcome.value = FetchOutcome.Failed(error)
            } finally {
                _isRetrying.value = false
            }
        }
    }

    private sealed interface FetchOutcome {
        data class Failed(val cause: Throwable) : FetchOutcome

        /** The feed has no such date. Retrying this cannot help. */
        data object NotFound : FetchOutcome
    }

    private companion object {
        const val TAG = "PlanetDetailViewModel"
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /** Nothing in the cache yet; the screen can still fetch the record. */
    data object Empty : DetailUiState

    /** The feed carries no record for this date: there is nothing left to fetch. */
    data object NotFound : DetailUiState

    data class Error(val cause: Throwable) : DetailUiState

    data class Content(val planet: PlanetUi) : DetailUiState
}
