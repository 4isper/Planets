package com.m4isper.domain.usecase

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetUseCasesTest {

    @Test
    fun `GetPlanetsUseCase combines items with the reached position`() = runBlocking {
        val repository = FakePlanetRepository(pagination = Pagination(currentPage = 2, totalPages = 10))

        val feed = GetPlanetsUseCase(repository)().first()

        assertEquals(listOf(samplePlanet), feed.items)
        assertEquals(2, feed.pagination?.currentPage)
    }

    @Test
    fun `LoadMorePlanetsUseCase reports the new position`() = runBlocking {
        val repository = FakePlanetRepository(pagination = Pagination(currentPage = 2, totalPages = 10))

        val result = LoadMorePlanetsUseCase(repository)(Pagination(currentPage = 2, totalPages = 10))

        assertEquals(Pagination(currentPage = 3, totalPages = 10), (result as LoadMoreResult.Loaded).pagination)
    }

    @Test
    fun `LoadMorePlanetsUseCase reports the end of the archive`() = runBlocking {
        val repository = FakePlanetRepository(pagination = null)

        val result = LoadMorePlanetsUseCase(repository)(Pagination(currentPage = 10, totalPages = 10))

        assertTrue(result is LoadMoreResult.EndReached)
    }

    @Test
    fun `RefreshPlanetsUseCase delegates to repository`() = runBlocking {
        val repository = FakePlanetRepository()
        RefreshPlanetsUseCase(repository)()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `GetPlanetDetailsUseCase returns planet by date`() = runBlocking {
        assertEquals(samplePlanet, GetPlanetDetailsUseCase(FakePlanetRepository())("2026-10-12").first())
    }

    private class FakePlanetRepository(
        private val pagination: Pagination? = null,
    ) : PlanetRepository {
        var refreshCalls = 0

        override fun observePlanets(): Flow<List<Planet>> = flowOf(listOf(samplePlanet))

        override fun observePagination(): Flow<Pagination?> = flowOf(pagination)

        override suspend fun refresh() {
            refreshCalls++
        }

        override suspend fun loadMore(after: Pagination): Pagination? =
            if (after.hasMore) after.advanced() else null

        override fun getPlanet(date: String): Flow<Planet?> = flowOf(samplePlanet)
    }

    private companion object {
        val samplePlanet = Planet(
            date = "2026-10-12",
            title = "Jupiter and the Moons",
            explanation = "Explanation text",
            mediaType = "image",
            credit = "Chirag Upreti",
            imageDescription = "Jupiter and its moons",
            permalink = "https://science.nasa.gov/image-article/apod-2026/",
            imageUrl = "https://assets.science.nasa.gov/dynamicimage/jupiter.jpg",
        )
    }
}