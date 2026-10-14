package com.m4isper.planets.presentation.list

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.usecase.GetPlanetsUseCase
import com.m4isper.domain.usecase.LoadMorePlanetsUseCase
import com.m4isper.domain.usecase.RefreshPlanetsUseCase
import com.m4isper.planets.presentation.FakePlanetRepository
import com.m4isper.planets.presentation.MainDispatcherRule
import com.m4isper.planets.presentation.FakePlanetRepository.Companion.SAMPLE
import com.m4isper.planets.presentation.FakePlanetRepository.Companion.networkError
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.model.toUi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanetsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial refresh loads the first page`() = runTest {
        val repository = FakePlanetRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
        assertTrue(viewModel.uiState.value is ListUiState.Content)
        assertEquals(listOf(SAMPLE.toUi()), viewModel.uiState.value.contentItems())
    }

    @Test
    fun `failed first load exposes the cause`() = runTest {
        val repository = FakePlanetRepository().apply { refreshFailure = networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ListUiState.Error)
        assertSame(networkError().javaClass, (state as ListUiState.Error).cause.javaClass)
    }

    @Test
    fun `retry after a failure loads content`() = runTest {
        val repository = FakePlanetRepository().apply { refreshFailure = networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        repository.refreshFailure = null
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.refreshCalls)
        assertTrue(viewModel.uiState.value is ListUiState.Content)
    }

    @Test
    fun `refresh is ignored while a page is being appended`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakePlanetRepository().apply { loadMoreGate = gate }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        // A pull to refresh during an in-flight page load must not reset the cache.
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
        assertTrue((viewModel.uiState.value as ListUiState.Content).isAppending)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `repeated loadMore calls do not overlap`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakePlanetRepository().apply { loadMoreGate = gate }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, repository.loadMoreCalls)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `loadMore is ignored without a reached position`() = runTest {
        val repository = FakePlanetRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        // The cache survives but the position is gone: nothing should reach the repository.
        repository.pagination.value = null
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(0, repository.loadMoreCalls)
        assertTrue(viewModel.uiState.value is ListUiState.Content)
    }

    @Test
    fun `failed page load keeps the content and offers a retry`() = runTest {
        val repository = FakePlanetRepository().apply { loadMoreFailure = networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ListUiState.Content
        assertFalse(state.isAppending)
        assertTrue(state.appendError)
        assertEquals(listOf(SAMPLE.toUi()), viewModel.uiState.value.contentItems())
    }

    @Test
    fun `end of archive leaves no append error`() = runTest {
        val repository = FakePlanetRepository().apply { endReached = true }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ListUiState.Content
        assertFalse(state.appendError)
        assertEquals(1, repository.loadMoreCalls)
    }

    @Test
    fun `a warm cache survives a failed refresh and still pages`() = runTest {
        val repository = FakePlanetRepository().apply {
            items.value = listOf(SAMPLE)
            pagination.value = Pagination(currentPage = 3, totalPages = 20)
            refreshFailure = networkError()
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        // The list is on screen, so it must stay interactive.
        val state = viewModel.uiState.value
        assertTrue(state is ListUiState.Content)
        assertEquals(listOf(SAMPLE.toUi()), viewModel.uiState.value.contentItems())
        assertFalse((state as ListUiState.Content).appendError)

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, repository.loadMoreCalls)
    }

    @Test
    fun `a cache that arrives after the refresh failed is still content`() = runTest {
        val repository = FakePlanetRepository().apply { refreshFailure = networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ListUiState.Error)

        // The cache lands after the network has already given up. The state has to
        // follow the cache, not the order the two happened to arrive in.
        repository.items.value = listOf(SAMPLE)
        repository.pagination.value = Pagination(currentPage = 1, totalPages = 20)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ListUiState.Content)
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(1, repository.loadMoreCalls)
    }

    @Test
    fun `refresh is ignored while the initial refresh is still in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakePlanetRepository().apply { loadMoreGate = gate }
        val viewModel = createViewModel(repository)

        // `init` already claimed the slot; three more pulls in the same frame
        // must not start a second request.
        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(repository: FakePlanetRepository): PlanetsListViewModel {
        val viewModel = PlanetsListViewModel(
            getPlanets = GetPlanetsUseCase(repository),
            refreshPlanets = RefreshPlanetsUseCase(repository),
            loadMorePlanets = LoadMorePlanetsUseCase(repository),
        )
        // The UI collects uiState and nothing else.
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }
}

private val SAMPLE = SAMPLE

private fun networkError() = networkError()

/** The list the screen renders, read through the only state it consumes. */
private fun ListUiState.contentItems(): List<PlanetUi> =
    (this as? ListUiState.Content)?.items ?: emptyList()
