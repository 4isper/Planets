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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlanetsListViewModel @Inject constructor(
    getPlanets: GetPlanetsUseCase,
    private val refreshPlanets: RefreshPlanetsUseCase,
    private val loadMorePlanets: LoadMorePlanetsUseCase,
) : ViewModel() {

    private val feed: StateFlow<PlanetsFeed> = getPlanets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY_FEED)

    /** What the last request settled on. The cache lives beside it, never inside it. */
    private val request = MutableStateFlow<Request>(Request.Idle)

    /**
     * What to render, derived rather than remembered.
     *
     * Derived from the cache and the request status rather than updated by them, so
     * that what is on screen never depends on how the last request ended, nor on the
     * order in which the cache and the network happened to answer. `Content` carries
     * its own records, which is what lets the screen read exactly one flow.
     */
    val uiState: StateFlow<ListUiState> = combine(feed, request) { feed, request ->
        when {
            feed.items.isNotEmpty() -> ListUiState.Content(
                items = feed.items.map { it.toUi() },
                isRefreshing = request is Request.Refreshing,
                isAppending = request is Request.Appending,
                appendError = request is Request.AppendFailed,
            )

            request is Request.RefreshFailed -> ListUiState.Error(request.cause)
            // Nothing cached and nothing settled yet: the initial refresh is either
            // running or about to, and there is nothing to show either way.
            else -> ListUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState.Loading,
    )

    init {
        refresh()
    }

    fun refresh() {
        // Claim the slot synchronously: the guard and the flag must be atomic,
        // otherwise two calls in the same frame both pass the guard.
        if (request.value.isBusy) return
        request.value = Request.Refreshing

        viewModelScope.launch {
            try {
                refreshPlanets()
                request.value = Request.Idle
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                report(error)
                request.value = Request.RefreshFailed(error)
            }
        }
    }

    fun loadMore() {
        if (request.value.isBusy) return

        // The domain owns the stop condition; without a reached position there is nothing to continue from.
        val pagination = feed.value.pagination ?: return

        request.value = Request.Appending

        viewModelScope.launch {
            try {
                val result = loadMorePlanets(pagination)
                request.value = Request.Idle
                if (result is LoadMoreResult.EndReached) {
                    Log.i(TAG, "Archive reached, no more pages")
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                report(error)
                request.value = Request.AppendFailed(error)
            }
        }
    }

    private fun report(error: Throwable) {
        Log.e(TAG, "Failed to load planets", error)
    }

    /** The status of the last request. Deliberately unable to say what is on screen. */
    private sealed interface Request {
        data object Idle : Request

        data object Refreshing : Request

        data object Appending : Request

        data class RefreshFailed(val cause: Throwable) : Request

        data class AppendFailed(val cause: Throwable) : Request

        val isBusy: Boolean get() = this is Refreshing || this is Appending
    }

    private companion object {
        const val TAG = "PlanetsListViewModel"

        val EMPTY_FEED = PlanetsFeed(items = emptyList(), pagination = null)
    }
}

/**
 * The list is either populated or empty (and then loading or failed).
 *
 * [Content] carries its own records, so the screen has exactly one thing to read and
 * cannot pair the list with a status that was decided somewhere else. An empty
 * [Content] is therefore not representable: the two facts are the same fact.
 */
sealed interface ListUiState {
    data object Loading : ListUiState

    data class Error(val cause: Throwable) : ListUiState

    data class Content(
        val items: List<PlanetUi>,
        val isRefreshing: Boolean = false,
        val isAppending: Boolean = false,
        val appendError: Boolean = false,
    ) : ListUiState
}