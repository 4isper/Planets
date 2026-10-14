package com.m4isper.remote.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ApodRecordTest {

    @Test
    fun `renders the date as the six digit path segment the route is keyed on`() {
        assertEquals("261012", "2026-10-12".toApodPathSegment())
        assertEquals("240315", "2024-03-15".toApodPathSegment())
        assertEquals("950616", "1995-06-16".toApodPathSegment())
    }

    @Test
    fun `keeps the century boundary unambiguous`() {
        // The segment is a path component, not a number: 1995 must not collapse
        // into the same segment as 2095.
        assertEquals("950101", "1995-01-01".toApodPathSegment())
        assertEquals("990101", "1999-01-01".toApodPathSegment())
        assertEquals("000101", "2000-01-01".toApodPathSegment())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a date that is not yyyy-MM-dd`() {
        // The route would answer `rest_no_route`; failing here keeps the miss local.
        "2026-10-12T00:00:00".toApodPathSegment()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects the path segment form itself`() {
        "261012".toApodPathSegment()
    }
}