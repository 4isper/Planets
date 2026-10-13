package com.m4isper.domain.usecase

import com.m4isper.domain.model.Planet
import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetPlanetDetailsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    operator fun invoke(date: String): Flow<Planet?> = repository.getPlanet(date)
}
