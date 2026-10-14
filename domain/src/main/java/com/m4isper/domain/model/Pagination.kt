package com.m4isper.domain.model

/**
 * Position of the feed inside the archive.
 *
 * The stop condition is a rule of the feed, not of the transport, so it lives here:
 * the repository and the UI must not each re-derive "is there another page".
 */
data class Pagination(
    val currentPage: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = currentPage < totalPages

    val nextPage: Int get() = currentPage + 1

    fun advanced(): Pagination = copy(currentPage = nextPage)

    /**
     * The position after a page that yielded [loadedCount] records, or `null` once the
     * archive is over.
     *
     * An empty page ends the archive regardless of [totalPages]: that count is a
     * header the server may omit or understate, so it cannot be the only stop
     * condition. A page of records already in the cache still counts — it advanced the
     * position even though nothing new was added.
     */
    fun advancedBy(loadedCount: Int): Pagination? =
        if (loadedCount <= 0 || !hasMore) null else advanced()

    companion object {
        const val INITIAL_PAGE = 1

        fun first(totalPages: Int): Pagination =
            Pagination(currentPage = INITIAL_PAGE, totalPages = totalPages)
    }
}