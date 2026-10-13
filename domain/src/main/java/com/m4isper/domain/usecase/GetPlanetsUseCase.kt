package com.m4isper.domain.usecase

import com.m4isper.domain.model.PlanetsFeed
import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetPlanetsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    /** The list is always rendered from cache, together with the reached position. */
    operator fun invoke(): Flow<PlanetsFeed> =
        combine(repository.observePlanets(), repository.observePagination()) { items, pagination ->
            PlanetsFeed(items = items, pagination = pagination)
        }
}