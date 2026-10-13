package com.m4isper.planets.presentation.list

import com.m4isper.domain.usecase.GetPlanetsUseCase
import com.m4isper.domain.usecase.LoadMorePlanetsUseCase
import com.m4isper.domain.usecase.RefreshPlanetsUseCase
import com.m4isper.planets.presentation.MainDispatcherRule
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
        assertEquals(listOf(FakePlanetRepository.SAMPLE.toUi()), viewModel.items.value)
    }

    @Test
    fun `failed first load exposes the cause`() = runTest {
        val repository = FakePlanetRepository().apply { refreshFailure = FakePlanetRepository.networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ListUiState.Error)
        assertSame(FakePlanetRepository.networkError().javaClass, (state as ListUiState.Error).cause.javaClass)
    }

    @Test
    fun `retry after a failure loads content`() = runTest {
        val repository = FakePlanetRepository().apply { refreshFailure = FakePlanetRepository.networkError() }
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
        val repository = FakePlanetRepository().apply { loadMoreFailure = FakePlanetRepository.networkError() }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ListUiState.Content
        assertFalse(state.isAppending)
        assertTrue(state.appendError)
        assertEquals(listOf(FakePlanetRepository.SAMPLE.toUi()), viewModel.items.value)
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

    private fun TestScope.createViewModel(repository: FakePlanetRepository): PlanetsListViewModel {
        val viewModel = PlanetsListViewModel(
            getPlanets = GetPlanetsUseCase(repository),
            refreshPlanets = RefreshPlanetsUseCase(repository),
            loadMorePlanets = LoadMorePlanetsUseCase(repository),
        )
        // The UI always collects both flows.
        backgroundScope.launch { viewModel.items.collect {} }
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }
}