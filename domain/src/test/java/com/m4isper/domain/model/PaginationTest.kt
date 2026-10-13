package com.m4isper.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginationTest {

    @Test
    fun `hasMore is true while the archive is not exhausted`() {
        assertTrue(Pagination(currentPage = 1, totalPages = 3).hasMore)
        assertTrue(Pagination(currentPage = 2, totalPages = 3).hasMore)
    }

    @Test
    fun `hasMore is false on the last page`() {
        assertFalse(Pagination(currentPage = 3, totalPages = 3).hasMore)
        assertFalse(Pagination(currentPage = 4, totalPages = 3).hasMore)
    }

    @Test
    fun `advanced moves to the next page`() {
        val pagination = Pagination(currentPage = 1, totalPages = 10).advanced()

        assertEquals(2, pagination.currentPage)
        assertEquals(10, pagination.totalPages)
    }

    @Test
    fun `first starts at the initial page`() {
        val pagination = Pagination.first(totalPages = 5)

        assertEquals(Pagination.INITIAL_PAGE, pagination.currentPage)
        assertTrue(pagination.hasMore)
    }

    @Test
    fun `single page archive is immediately exhausted`() {
        assertFalse(Pagination.first(totalPages = 1).hasMore)
    }
}