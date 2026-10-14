package com.m4isper.planets.ui.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Tag used for click lookup in `ClickableText`. */
const val URL_ANNOTATION_TAG = "URL"

private val BOLD = SpanStyle(fontWeight = FontWeight.Bold)
private val ITALIC = SpanStyle(fontStyle = FontStyle.Italic)
private val LINK = SpanStyle(textDecoration = TextDecoration.Underline)

private val INLINE_SPACE = Regex("\\s+")

/**
 * Renders an APOD HTML fragment (`<strong>`, `<em>`, `<a href>`, `<br>`, `<p>`)
 * as styled text with clickable links. Jsoup decodes entities and tolerates
 * malformed markup; unknown tags are flattened to their text.
 *
 * Plain strings cached by older versions contain no tags and come back unchanged.
 */
fun String.toRichText(): AnnotatedString {
    if (this.isBlank()) return AnnotatedString("")
    val body = Jsoup.parseBodyFragment(this).body()
    val writer = RichWriter()
    writer.appendBlockChildren(body)
    return writer.toTrimmed()
}

/** [AnnotatedString.Builder] wrapper tracking the last char for newline guards. */
private class RichWriter {
    private val builder = AnnotatedString.Builder()
    private var last: Char? = null

    fun toTrimmed(): AnnotatedString {
        val built = builder.toAnnotatedString()
        var start = 0
        while (start < built.text.length && (built.text[start] == '\n' || built.text[start] == ' ')) start++
        var end = built.text.length
        while (end > start && (built.text[end - 1] == '\n' || built.text[end - 1] == ' ')) end--
        return if (start == 0 && end == built.text.length) built else built.subSequence(start, end)
    }

    fun appendBlockChildren(element: Element) {
        for (child in element.childNodes()) {
            if (child is Element && child.isBlockElement()) {
                if (last != null && last != '\n') appendRaw("\n\n")
                appendNode(child)
                if (last != '\n') appendRaw("\n\n")
            } else {
                appendNode(child)
            }
        }
    }

    fun appendNode(node: Node) {
        when (node) {
            is TextNode -> appendInlineText(node.wholeText)
            is Element -> when (node.tagName().lowercase()) {
                "br" -> appendRaw("\n")
                "strong", "b" -> {
                    builder.pushStyle(BOLD)
                    appendInlineChildren(node)
                    builder.pop()
                }
                "em", "i" -> {
                    builder.pushStyle(ITALIC)
                    appendInlineChildren(node)
                    builder.pop()
                }
                "a" -> {
                    val href = node.attr("href").trim()
                    if (href.isEmpty()) {
                        appendInlineChildren(node)
                    } else {
                        val start = builder.length
                        builder.pushStyle(LINK)
                        builder.pushLink(LinkAnnotation.Url(href))
                        appendInlineChildren(node)
                        builder.pop()
                        builder.pop()
                        builder.addStringAnnotation(URL_ANNOTATION_TAG, href, start, builder.length)
                        if (builder.length > start) last = lastOfBuilder()
                    }
                }
                else -> {
                    if (node.isBlockElement()) {
                        if (last != null && last != '\n') appendRaw("\n\n")
                        appendInlineChildren(node)
                        if (last != '\n') appendRaw("\n\n")
                    } else {
                        appendInlineChildren(node)
                    }
                }
            }
        }
    }

    fun appendInlineChildren(element: Element) {
        for (child in element.childNodes()) {
            if (child is Element && child.isBlockElement()) {
                if (last != null && last != '\n') appendRaw("\n\n")
                appendNode(child)
                if (last != '\n') appendRaw("\n\n")
            } else {
                appendNode(child)
            }
        }
    }

    private fun appendInlineText(raw: String) {
        var text = raw.replace('\u00a0', ' ').replace(INLINE_SPACE, " ")
        if (text.isEmpty()) return
        if (text[0] == ' ' && (last == null || last == ' ' || last == '\n')) {
            text = text.drop(1)
        }
        if (text.isEmpty()) return
        appendRaw(text)
    }

    private fun appendRaw(text: String) {
        if (text.isEmpty()) return
        builder.append(text)
        last = text.last()
    }

    private fun lastOfBuilder(): Char? {
        val built = builder.toAnnotatedString()
        return built.text.lastOrNull()?.also { last = it }
    }
}

private fun Element.isBlockElement(): Boolean = when (tagName().lowercase()) {
    "p", "div", "section", "article", "ul", "ol", "li",
    "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "hr" -> true
    else -> false
}
