package com.m4isper.repository.mappers

import com.m4isper.local.entity.ApodEntity
import com.m4isper.remote.model.ApodResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApodMappersTest {

    @Test
    fun `toEntity maps fields and strips html`() {
        val dto = ApodResponse(
            date = "2026-10-12",
            title = "Jupiter and the Moons",
            mediaType = "image",
            explanation = "<strong>Explanation:</strong> How many moons?",
            credit = "<a href=\"https://x.com\">Chirag Upreti</a>",
            alt = "Jupiter and four of its moons",
            permalink = "https://science.nasa.gov/image-article/apod-2026/",
            hdUrl = "https://assets.science.nasa.gov/dynamicimage/jupiter.jpg",
            url = "https://science.nasa.gov/image-article/apod-2026/",
        )

        val entity = dto.toEntity(page = 1, position = 0)

        assertEquals("2026-10-12", entity?.date)
        assertEquals("Jupiter and the Moons", entity?.title)
        assertEquals("Explanation: How many moons?", entity?.explanation)
        assertEquals("Chirag Upreti", entity?.credit)
        assertEquals("Jupiter and four of its moons", entity?.alt)
        assertEquals("image", entity?.mediaType)
        assertEquals(
            "https://assets.science.nasa.gov/dynamicimage/jupiter.jpg",
            entity?.imageUrl,
        )
        assertEquals("https://science.nasa.gov/image-article/apod-2026/", entity?.permalink)
        assertEquals(1, entity?.page)
    }

    @Test
    fun `toEntity returns null when date is missing`() {
        assertNull(ApodResponse(date = null, title = "Title").toEntity(page = 1, position = 0))
    }

    @Test
    fun `toEntity falls back to url when hdurl is absent`() {
        val dto = ApodResponse(date = "2026-10-12", url = "https://example.com/a.jpg")

        assertEquals("https://example.com/a.jpg", dto.toEntity(page = 1, position = 0)?.imageUrl)
    }

    @Test
    fun `toDomain maps entity and detects video`() {
        val entity = ApodEntity(
            date = "2026-10-12",
            title = null,
            explanation = null,
            mediaType = "video",
            credit = null,
            alt = null,
            permalink = null,
            imageUrl = null,
            page = 1,
            position = 0,
        )

        val planet = entity.toDomain()

        assertEquals("", planet.title)
        assertEquals("", planet.explanation)
        assertEquals(true, planet.isVideo)
    }
}