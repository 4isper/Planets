package com.m4isper.domain.repository

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.model.Planet
import kotlinx.coroutines.flow.Flow

interface PlanetRepository {

    fun observePlanets(): Flow<List<Planet>>

    /** Position reached so far, or `null` when nothing has been loaded yet. */
    fun observePagination(): Flow<Pagination?>

    suspend fun refresh()

    /**
     * Loads the page that follows [after].
     *
     * @return the new position in the archive, or `null` when the archive is exhausted.
     */
    suspend fun loadMore(after: Pagination): Pagination?

    fun getPlanet(date: String): Flow<Planet?>

    /**
     * Fetches one record from the network and caches it.
     *
     * @return `false` when the feed carries no record for [date] — a settled
     *   answer, as opposed to a failure that a retry might get past.
     */
    suspend fun fetchPlanet(date: String): Boolean
}