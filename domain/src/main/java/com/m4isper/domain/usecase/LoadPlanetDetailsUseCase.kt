package com.m4isper.domain.usecase

import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject

/**
 * Recovers a details screen when the record is not in the cache.
 *
 * The details screen is driven by the local cache, so a restored back stack or a
 * cleared cache leaves it with nothing to show; this fetches the record directly.
 */
class LoadPlanetDetailsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    suspend operator fun invoke(date: String) = repository.fetchPlanet(date)
}