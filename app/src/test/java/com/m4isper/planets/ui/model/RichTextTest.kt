package com.m4isper.planets.ui.model

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {

    @Test
    fun `plain text passes through`() {
        assertEquals("Explanation text", "Explanation text".toRichText().text)
    }

    @Test
    fun `bold tag becomes span`() {
        val rich = "<strong>Explanation:</strong> How many moons?".toRichText()

        assertEquals("Explanation: How many moons?", rich.text)
        val bold = rich.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertTrue(bold != null && bold.start == 0 && bold.end == 12)
    }

    @Test
    fun `link becomes clickable annotation`() {
        val rich = "See <a href=\"https://nasa.gov\">Moon</a> &amp; Sun".toRichText()

        assertEquals("See Moon & Sun", rich.text)
        val links = rich.getStringAnnotations(URL_ANNOTATION_TAG, 0, rich.length)
        assertEquals(1, links.size)
        assertEquals("https://nasa.gov", links[0].item)
        assertEquals("See ".length, links[0].start)
    }

    @Test
    fun `br becomes newline`() {
        assertEquals("A\nB", "A<br>B".toRichText().text)
    }

    @Test
    fun `empty stays empty`() {
        assertEquals("", "".toRichText().text)
    }
}
