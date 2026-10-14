package com.m4isper.planets.presentation

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/**
 * Hand written double for the whole repository contract.
 *
 * One fake for every ViewModel test: two copies used to drift apart, and the one in
 * the detail tests answered `observePagination()` with a cold flow where Room returns
 * a live one, so tests could pass against behaviour the app never has.
 *
 * Cache access mirrors Room — flows that re-emit on every change, including the
 * record currently held for the details screen.
 */
class FakePlanetRepository : PlanetRepository {

    val items = MutableStateFlow<List<Planet>>(emptyList())
    val pagination = MutableStateFlow<Pagination?>(null)

    var refreshCalls = 0
        private set

    var loadMoreCalls = 0
        private set

    var fetchCalls = 0
        private set

    var refreshFailure: Throwable? = null

    var loadMoreFailure: Throwable? = null

    var fetchFailure: Throwable? = null

    /** When false [fetchPlanet] reports that the feed has no such date. */
    var fetchSucceeds = true

    /** When true [loadMore] reports the end of the archive. */
    var endReached = false

    /** Held open to simulate a request that is still in flight. */
    var loadMoreGate: CompletableDeferred<Unit>? = null

    /** The record [getPlanet] currently serves, addressed by date. */
    var cached: Planet? = null

    fun seed(vararg planets: Planet) {
        items.value = planets.toList()
    }

    fun cache(planet: Planet) {
        cached = planet
        items.value = items.value.filterNot { it.date == planet.date } + planet
    }

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
        items.update { it + SAMPLE.copy(date = "2026-10-1${loadMoreCalls}") }
        pagination.value = advanced
        return advanced
    }

    override fun getPlanet(date: String): Flow<Planet?> =
        items.map { planets -> planets.firstOrNull { it.date == date } }

    override suspend fun fetchPlanet(date: String): Boolean {
        fetchCalls++
        fetchFailure?.let { throw it }
        return fetchSucceeds
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