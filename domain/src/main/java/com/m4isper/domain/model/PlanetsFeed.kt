package com.m4isper.domain.model

/** What the list screen needs: the cached items plus the position in the archive. */
data class PlanetsFeed(
    val items: List<Planet>,
    val pagination: Pagination?,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}