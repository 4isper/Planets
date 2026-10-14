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
    "ldquo" to "“",
    "rdquo" to "”",
    "lsquo" to "‘",
    "rsquo" to "’",
    "laquo" to "«",
    "raquo" to "»",
    "copy" to "©",
    "reg" to "®",
    "trade" to "™",
    "deg" to "°",
    "plusmn" to "±",
    "times" to "×",
    "divide" to "÷",
    "middot" to "·",
    "bull" to "•",
    "prime" to "′",
    "Prime" to "″",
    "euro" to "€",
    "sect" to "§",
    "para" to "¶",
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

            else -> NAMED_ENTITIES[body] ?: NAMED_ENTITIES[body.lowercase()] ?: match.value
        }
    }
    return decoded.replace(WHITESPACE_PATTERN, " ").trim()
}

private val APOD_BOILERPLATE = listOf(
    "APOD's main NASA site is moving",
    "Tomorrow's picture:",
)

private val APOSTROPHES = setOf('’', '‘', '`', '´', 'ʼ', 'ʹ')

private fun String.normalizeApostrophes(): String {
    var result = this
    for (ch in APOSTROPHES) result = result.replace(ch, '\'')
    return result
}

private val TRAILING_TAGS = Regex("(\\s*<[^>]*>)+\\s*$")

/**
 * The feed appends site navigation to every `explanation`; cut it off
 * so the UI shows only the astronomy text.
 *
 * Raw variant: cuts the same markers but keeps the HTML tags before the cut,
 * so the UI can render bold/links with Jsoup. Orphaned opening tags left
 * behind by the cut (e.g. `... <br><br><strong>`) are dropped.
 */
fun String?.cutApodBoilerplateHtml(): String {
    if (this.isNullOrBlank()) return ""
    val normalized = normalizeApostrophes()
    var end = normalized.length
    for (marker in APOD_BOILERPLATE) {
        val index = normalized.indexOf(marker)
        if (index >= 0 && index < end) end = index
    }
    return this.substring(0, end.coerceAtMost(this.length))
        .replace(TRAILING_TAGS, "")
        .trim()
}

/** Plain-text variant: strips tags first, then cuts the same boilerplate. */
fun String?.stripApodHtml(): String {
    var text = stripHtml().normalizeApostrophes()
    for (marker in APOD_BOILERPLATE) {
        val index = text.indexOf(marker)
        if (index >= 0) {
            text = text.substring(0, index)
        }
    }
    return text.trim()
}