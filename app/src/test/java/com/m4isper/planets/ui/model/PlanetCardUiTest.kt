package com.m4isper.planets.ui.model

import com.m4isper.domain.model.Planet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetCardUiTest {

    @Test
    fun `card carries title image and media flag`() {
        val card = SAMPLE.toCardUi()

        assertEquals("2026-10-12", card.date)
        assertEquals("Jupiter and the Moons", card.title)
        assertEquals("https://assets.science.nasa.gov/dynamicimage/jupiter.jpg", card.imageUrl)
        assertEquals("Jupiter and four of its moons", card.imageContentDescription)
        assertTrue(card.hasImage)
        assertFalse(card.isVideo)
    }

    @Test
    fun `card falls back to date and flags video`() {
        val card = SAMPLE.copy(
            title = "  ",
            imageUrl = null,
            mediaType = "iframe",
        ).toCardUi()

        assertEquals("2026-10-12", card.title)
        assertFalse(card.hasImage)
        assertTrue(card.isVideo)
    }

    companion object {
        private val SAMPLE = Planet(
            date = "2026-10-12",
            title = "Jupiter and the Moons",
            explanation = "<strong>Explanation:</strong> How many moons?",
            mediaType = "image",
            credit = "<a href=\"https://x.com\">Chirag Upreti</a>",
            imageDescription = "Jupiter and four of its moons",
            permalink = "https://science.nasa.gov/image-article/apod-2026/",
            imageUrl = "https://assets.science.nasa.gov/dynamicimage/jupiter.jpg",
        )
    }
}
