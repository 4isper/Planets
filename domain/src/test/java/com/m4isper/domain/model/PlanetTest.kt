package com.m4isper.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetTest {

    @Test
    fun `displayTitle falls back to the date when the title is blank`() {
        assertEquals("2026-10-12", planet(title = "  ").displayTitle)
        assertEquals("Title", planet(title = "Title").displayTitle)
    }

    @Test
    fun `video records are detected by media type`() {
        assertTrue(planet(mediaType = "video").isVideo)
        assertFalse(planet(mediaType = "image").isVideo)
    }

    @Test
    fun `hasImage requires a non blank url`() {
        assertTrue(planet(imageUrl = "https://x/y.jpg").hasImage)
        assertFalse(planet(imageUrl = null).hasImage)
        assertFalse(planet(imageUrl = "  ").hasImage)
    }

    @Test
    fun `isDisplayable requires an image and a title`() {
        assertTrue(planet().isDisplayable)
        assertFalse(planet(imageUrl = null).isDisplayable)
    }

    @Test
    fun `imageContentDescription prefers the apod alt text`() {
        assertEquals(
            "Jupiter and four of its moons",
            planet(imageDescription = "Jupiter and four of its moons").imageContentDescription,
        )
    }

    @Test
    fun `imageContentDescription falls back to the display title`() {
        assertEquals("Title", planet(imageDescription = null).imageContentDescription)
        assertEquals("Title", planet(imageDescription = "  ").imageContentDescription)
        assertEquals("2026-10-12", planet(title = "", imageDescription = null).imageContentDescription)
    }

    private fun planet(
        title: String = "Title",
        mediaType: String = "image",
        imageUrl: String? = "https://x/y.jpg",
        imageDescription: String? = null,
    ) = Planet(
        date = "2026-10-12",
        title = title,
        explanation = "Explanation",
        mediaType = mediaType,
        credit = null,
        imageDescription = imageDescription,
        permalink = null,
        imageUrl = imageUrl,
    )
}