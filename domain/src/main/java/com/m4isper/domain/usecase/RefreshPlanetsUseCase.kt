package com.m4isper.domain.usecase

import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject

class RefreshPlanetsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
