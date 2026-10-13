package com.m4isper.planets.presentation.list

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

/**
 * Hand written double: the use cases take the repository through their constructor,
 * so no mocking framework is needed.
 */
class FakePlanetRepository(
    val items: MutableStateFlow<List<Planet>> = MutableStateFlow(emptyList()),
    val pagination: MutableStateFlow<Pagination?> = MutableStateFlow(null),
) : PlanetRepository {

    var fetchCalls = 0
        private set

    var fetchFailure: Throwable? = null

    var refreshCalls = 0
        private set

    var loadMoreCalls = 0
        private set

    var refreshFailure: Throwable? = null

    var loadMoreFailure: Throwable? = null

    /** When true [loadMore] reports the end of the archive. */
    var endReached = false

    /** Held open to simulate a page request that is still in flight. */
    var loadMoreGate: CompletableDeferred<Unit>? = null

    override fun observePlanets(): Flow<List<Planet>> = items

    override fun observePagination(): Flow<Pagination?> = pagination

    override suspend fun refresh() {
        refreshCalls++
        refreshFailure?.let { throw it }
        items.value = listOf(SAMPLE)
        pagination.value = Pagination(currentPage = 1, totalPages = TOTAL_PAGES)
    }

    override suspend fun loadMore(after: Pagination): Pagination? {
        loadMoreCalls++
        loadMoreGate?.await()
        loadMoreFailure?.let { throw it }
        if (endReached) return null

        val advanced = after.advanced()
        items.value = items.value + SAMPLE.copy(date = "2026-10-1${loadMoreCalls}")
        pagination.value = advanced
        return advanced
    }

    override fun getPlanet(date: String): Flow<Planet?> = flowOf(items.value.firstOrNull())

    override suspend fun fetchPlanet(date: String) {
        fetchCalls++
        fetchFailure?.let { throw it }
    }

    companion object {
        const val TOTAL_PAGES = 5

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

        fun networkError() = IOException("offline")
    }
}