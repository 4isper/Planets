package com.m4isper.domain.usecase

import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject

sealed interface LoadPlanetDetailsResult {
    data object Fetched : LoadPlanetDetailsResult

    /** The feed has no record for this date: retrying cannot change that. */
    data object NotFound : LoadPlanetDetailsResult
}

/**
 * Recovers a details screen when the record is not in the cache.
 *
 * The details screen is driven by the local cache, so a restored back stack or a
 * cleared cache leaves it with nothing to show; this fetches the record directly.
 */
class LoadPlanetDetailsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    suspend operator fun invoke(date: String): LoadPlanetDetailsResult =
        if (repository.fetchPlanet(date)) {
            LoadPlanetDetailsResult.Fetched
        } else {
            LoadPlanetDetailsResult.NotFound
        }
}