package com.m4isper.repository.mappers

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun `strips tags and decodes entities`() {
        val html = "<strong>Explanation:</strong> Earth&#039;s <a href=\"https://nasa.gov\">Moon</a> &amp; Sun"

        assertEquals("Explanation: Earth's Moon & Sun", html.stripHtml())
    }

    @Test
    fun `collapses whitespace and trims`() {
        assertEquals("one two", "  one \n\n  two  ".stripHtml())
    }

    @Test
    fun `returns empty string for null and blank`() {
        assertEquals("", null.stripHtml())
        assertEquals("", "   ".stripHtml())
    }

    @Test
    fun `keeps unknown entities as is`() {
        assertEquals("&unknown;", "&unknown;".stripHtml())
    }

    @Test
    fun `decodes extended named entities`() {
        assertEquals("“hi” ‘yo’ © ° —", "&ldquo;hi&rdquo; &lsquo;yo&rsquo; &copy; &deg; &mdash;".stripHtml())
    }

    @Test
    fun `stripApodHtml cuts boilerplate with curly apostrophe`() {
        val html = "A nebula. <br><br><strong>Tomorrow’s picture: </strong><a href=\"#\">sun</a>"

        assertEquals("A nebula.", html.stripApodHtml())
    }

    @Test
    fun `stripApodHtml cuts navigation boilerplate`() {
        val html = "<strong>Explanation:</strong> A nebula. <br><br>" +
            "<strong>APOD's main NASA site is <a href=\"#\">moving</a>:</strong> " +
            "From apod.nasa.gov to science.nasa.gov/apod" +
            "<br><strong>Tomorrow's picture: </strong><a href=\"#\">only sunny days</a>"

        assertEquals("Explanation: A nebula.", html.stripApodHtml())
    }
}