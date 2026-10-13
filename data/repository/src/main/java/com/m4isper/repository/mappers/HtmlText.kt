package com.m4isper.repository.mappers

private val TAG_PATTERN = Regex("<[^>]+>")
private val ENTITY_PATTERN = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")
private val WHITESPACE_PATTERN = Regex("\\s+")

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "hellip" to "…",
    "mdash" to "—",
    "ndash" to "–",
)

/**
 * The science.nasa.gov APOD feed returns `explanation`, `credit` and `copyright`
 * as HTML fragments (`<strong>`, `<a>`, entities). Domain models keep plain text.
 */
fun String?.stripHtml(): String {
    if (this.isNullOrBlank()) return ""

    val withoutTags = replace(TAG_PATTERN, " ")
    val decoded = ENTITY_PATTERN.replace(withoutTags) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x", ignoreCase = true) ->
                body.drop(2).toIntOrNull(16)?.toChar()?.toString() ?: match.value

            body.startsWith("#") ->
                body.drop(1).toIntOrNull()?.toChar()?.toString() ?: match.value

            else -> NAMED_ENTITIES[body.lowercase()] ?: match.value
        }
    }
    return decoded.replace(WHITESPACE_PATTERN, " ").trim()
}

private val APOD_BOILERPLATE = listOf(
    "APOD's main NASA site is moving",
    "Tomorrow's picture:",
)

/**
 * The feed appends site navigation to every `explanation`; cut it off
 * so the UI shows only the astronomy text.
 */
fun String?.stripApodHtml(): String {
    var text = stripHtml()
    for (marker in APOD_BOILERPLATE) {
        val index = text.indexOf(marker)
        if (index > 0) {
            text = text.substring(0, index)
        }
    }
    return text.trim()
}