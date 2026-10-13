package com.m4isper.planets.presentation.detail

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import com.m4isper.domain.usecase.GetPlanetDetailsUseCase
import com.m4isper.domain.usecase.LoadPlanetDetailsUseCase
import com.m4isper.planets.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val repository = FakePlanetRepository(cached = SAMPLE)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Content)
        assertEquals(SAMPLE.date, (state as DetailUiState.Content).planet.title.let { SAMPLE.date })
    }

    @Test
    fun `missing cache entry ends in empty, not loading`() = runTest {
        val viewModel = createViewModel(FakePlanetRepository(cached = null))
        advanceUntilIdle()

        assertEquals(DetailUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `retry fetches the record and the cache update shows it`() = runTest {
        val repository = FakePlanetRepository(cached = null)
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
        val repository = FakePlanetRepository(cached = null).apply {
            fetchFailure = IOException("offline")
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Error)
        assertTrue((state as DetailUiState.Error).cause is IOException)
    }

    @Test
    fun `repeated retries do not overlap`() = runTest {
        val repository = FakePlanetRepository(cached = null)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, repository.fetchCalls)
    }

    private fun TestScope.createViewModel(repository: FakePlanetRepository) =
        PlanetDetailViewModel(
            date = SAMPLE.date,
            getPlanetDetails = GetPlanetDetailsUseCase(repository),
            loadPlanetDetails = LoadPlanetDetailsUseCase(repository),
        ).also { backgroundScope.launch { it.uiState.collect {} } }

    private class FakePlanetRepository(cached: Planet?) : PlanetRepository {

        var fetchCalls = 0
            private set

        var fetchFailure: Throwable? = null

        /** Room exposes the record as a Flow that re-emits on every cache change. */
        private val cached = MutableStateFlow(cached)
        private val items = MutableStateFlow<List<Planet>>(listOfNotNull(cached))

        fun cache(planet: Planet) {
            cached.value = planet
            items.value = listOf(planet)
        }

        override fun observePlanets(): Flow<List<Planet>> = items

        override fun observePagination(): Flow<Pagination?> = flowOf(null)

        override suspend fun refresh() = Unit

        override suspend fun loadMore(after: Pagination): Pagination? = null

        override fun getPlanet(date: String): Flow<Planet?> = cached

        override suspend fun fetchPlanet(date: String) {
            fetchCalls++
            fetchFailure?.let { throw it }
        }
    }

    private companion object {
        val SAMPLE = Planet(
            date = "2026-10-12",
            title = "Jupiter and the Moons",
            explanation = "Explanation text",
            mediaType = "image",
            credit = "NASA",
            imageDescription = "Jupiter and four of its moons",
            permalink = "https://science.nasa.gov/image-article/apod-2026/",
            imageUrl = "https://assets.science.nasa.gov/dynamicimage/jupiter.jpg",
        )
    }
}