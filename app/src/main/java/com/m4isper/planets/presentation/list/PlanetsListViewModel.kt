package com.m4isper.planets.presentation.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.domain.model.PlanetsFeed
import com.m4isper.domain.usecase.GetPlanetsUseCase
import com.m4isper.domain.usecase.LoadMorePlanetsUseCase
import com.m4isper.domain.usecase.LoadMoreResult
import com.m4isper.domain.usecase.RefreshPlanetsUseCase
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlanetsListViewModel @Inject constructor(
    getPlanets: GetPlanetsUseCase,
    private val refreshPlanets: RefreshPlanetsUseCase,
    private val loadMorePlanets: LoadMorePlanetsUseCase,
) : ViewModel() {

    private val feed: StateFlow<PlanetsFeed> = getPlanets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY_FEED)

    /** The only thing the screen sees: view ready records, never the domain model. */
    val items: StateFlow<List<PlanetUi>> = feed
        .map { current -> current.items.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        when (val state = _uiState.value) {
            is ListUiState.Content -> if (state.isBusy) return
            // Loading and Error are both refreshable: that is what retry means.
            is ListUiState.Loading, is ListUiState.Error -> Unit
        }

        val firstLoad = feed.value.isEmpty
        // Claim the state before launching: the guard and the flag must be atomic,
        // otherwise two calls in the same frame both pass the guard.
        _uiState.update {
            if (firstLoad) ListUiState.Loading else it.asContent().refreshing()
        }

        viewModelScope.launch {
            runCatching { refreshPlanets() }
                .onSuccess {
                    // A failed refresh must keep the content on screen.
                    _uiState.update { state ->
                        if (firstLoad) ListUiState.Content() else state.asContent().refreshing(false)
                    }
                }
                .onFailure { error ->
                    report(error)
                    _uiState.update { state ->
                        if (firstLoad) ListUiState.Error(error) else state.asContent().refreshing(false)
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is ListUiState.Content || state.isAppending) return

        // The domain owns the stop condition; without a reached position there is nothing to continue from.
        val pagination = feed.value.pagination ?: return

        _uiState.update { it.asContent().appending(isAppending = true, appendError = false) }

        viewModelScope.launch {
            runCatching { loadMorePlanets(pagination) }
                .onSuccess { result ->
                    _uiState.update { it.asContent().appending(isAppending = false) }
                    if (result is LoadMoreResult.EndReached) {
                        Log.i(TAG, "Archive reached, no more pages")
                    }
                }
                .onFailure { error ->
                    report(error)
                    _uiState.update { it.asContent().appending(isAppending = false, appendError = true) }
                }
        }
    }

    private fun ListUiState.asContent(): ListUiState.Content =
        this as? ListUiState.Content ?: ListUiState.Content()

    private fun report(error: Throwable) {
        Log.e(TAG, "Failed to load planets", error)
    }

    private companion object {
        const val TAG = "PlanetsListViewModel"

        val EMPTY_FEED = PlanetsFeed(items = emptyList(), pagination = null)
    }
}

/**
 * The list is either empty (and then loading or failed) or populated.
 * Concurrent request flags live only inside [Content], so the impossible
 * combinations cannot be constructed by the UI.
 */
sealed interface ListUiState {

    data object Loading : ListUiState

    data class Error(val cause: Throwable) : ListUiState

    data class Content(
        val isRefreshing: Boolean = false,
        val isAppending: Boolean = false,
        val appendError: Boolean = false,
    ) : ListUiState {
        /** A refresh must not overlap a page load, otherwise the cache swap races. */
        val isBusy: Boolean get() = isRefreshing || isAppending

        fun refreshing(value: Boolean = true): Content = copy(isRefreshing = value)

        fun appending(isAppending: Boolean, appendError: Boolean = this.appendError): Content =
            copy(isAppending = isAppending, appendError = appendError)
    }
}