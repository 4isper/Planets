package com.m4isper.planets.presentation.detail

import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.domain.usecase.LoadPlanetDetailsUseCase
import com.m4isper.planets.presentation.FakePlanetRepository
import com.m4isper.planets.presentation.MainDispatcherRule
import com.m4isper.planets.presentation.FakePlanetRepository.Companion.SAMPLE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PlanetDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cached record is shown`() = runTest {
        val viewModel = createViewModel(FakePlanetRepository().apply { cache(SAMPLE) })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Content)
        assertEquals(SAMPLE.title, (state as DetailUiState.Content).planet.title)
        assertEquals(SAMPLE.date, state.planet.date)
    }

    @Test
    fun `missing cache entry ends in empty, not loading`() = runTest {
        val viewModel = createViewModel(FakePlanetRepository())
        advanceUntilIdle()

        assertEquals(DetailUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a cached record for another date does not answer this one`() = runTest {
        val repository = FakePlanetRepository().apply {
            cache(SAMPLE.copy(date = "2026-10-11"))
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(DetailUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `retry fetches the record and the cache update shows it`() = runTest {
        val repository = FakePlanetRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        // The fetch fills the cache, the Room flow re-emits and the screen updates itself.
        repository.cache(SAMPLE)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, repository.fetchCalls)
        assertTrue(viewModel.uiState.value is DetailUiState.Content)
    }

    @Test
    fun `failed retry surfaces an error state`() = runTest {
        val repository = FakePlanetRepository().apply { fetchFailure = IOException("offline") }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Error)
        assertTrue((state as DetailUiState.Error).cause is IOException)
    }

    @Test
    fun `a date the feed does not carry is settled, not a retryable error`() = runTest {
        val repository = FakePlanetRepository().apply { fetchSucceeds = false }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(DetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `a later successful retry clears the earlier failure`() = runTest {
        val repository = FakePlanetRepository().apply { fetchFailure = IOException("offline") }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DetailUiState.Error)

        repository.fetchFailure = null
        repository.fetchSucceeds = true
        repository.cache(SAMPLE)
        viewModel.retry()
        advanceUntilIdle()

        // The stuck error would otherwise keep the record off the screen.
        assertTrue(viewModel.uiState.value is DetailUiState.Content)
    }

    @Test
    fun `repeated retries do not overlap`() = runTest {
        val repository = FakePlanetRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, repository.fetchCalls)
    }

    @Test
    fun `the retry flag clears once the fetch settles`() = runTest {
        val repository = FakePlanetRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.isRetrying.value)
    }

    private fun TestScope.createViewModel(repository: FakePlanetRepository) =
        PlanetDetailViewModel(
            date = SAMPLE.date,
            getPlanetDetails = GetPlanetDetailsUseCase(repository),
            loadPlanetDetails = LoadPlanetDetailsUseCase(repository),
        ).also { backgroundScope.launch { it.uiState.collect {} } }
}