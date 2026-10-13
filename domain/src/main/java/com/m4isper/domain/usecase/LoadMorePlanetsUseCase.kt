package com.m4isper.domain.usecase

import com.m4isper.domain.model.Pagination
import com.m4isper.domain.repository.PlanetRepository
import javax.inject.Inject

sealed interface LoadMoreResult {
    data class Loaded(val pagination: Pagination) : LoadMoreResult

    /** The archive is exhausted: the UI should stop asking for more. */
    data object EndReached : LoadMoreResult
}

class LoadMorePlanetsUseCase @Inject constructor(
    private val repository: PlanetRepository,
) {
    suspend operator fun invoke(after: Pagination): LoadMoreResult =
        repository.loadMore(after)
            ?.let { LoadMoreResult.Loaded(pagination = it) }
            ?: LoadMoreResult.EndReached
}