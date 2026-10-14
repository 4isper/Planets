package com.m4isper.repository.mappers

import com.m4isper.local.entity.ApodEntity
import com.m4isper.remote.model.ApodResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApodMappersTest {

    @Test
    fun `toEntity keeps raw html for rich rendering`() {
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
        assertEquals("<strong>Explanation:</strong> How many moons?", entity?.explanation)
        assertEquals("<a href=\"https://x.com\">Chirag Upreti</a>", entity?.credit)
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
    fun `toEntity cuts boilerplate but keeps tags`() {
        val dto = ApodResponse(
            date = "2026-10-12",
            explanation = "A nebula. <br><br><strong>Tomorrow's picture: </strong><a href=\"#\">sun</a>",
        )

        assertEquals("A nebula.", dto.toEntity(page = 1, position = 0)?.explanation)
    }

    @Test
    fun `toEntity falls back to copyright when credit is blank`() {
        val dto = ApodResponse(
            date = "2026-10-12",
            credit = "   ",
            copyright = "<a href=\"https://x.com\">Aldo Zanetti</a>",
        )

        assertEquals("<a href=\"https://x.com\">Aldo Zanetti</a>", dto.toEntity(page = 1, position = 0)?.credit)
    }

    @Test
    fun `toEntity keeps article url out of imageUrl for video`() {
        val dto = ApodResponse(
            date = "2026-10-12",
            mediaType = "video",
            permalink = "https://science.nasa.gov/image-article/apod-2026/",
            url = "https://science.nasa.gov/image-article/apod-2026/",
            hdUrl = null,
        )

        assertNull(dto.toEntity(page = 1, position = 0)?.imageUrl)
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