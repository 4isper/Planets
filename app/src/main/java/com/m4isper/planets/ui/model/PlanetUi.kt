package com.m4isper.planets.ui.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.m4isper.domain.model.Planet

/**
 * Everything a screen needs to render a record.
 *
 * Composables never see [Planet]: the domain model stays an implementation detail of
 * the presentation layer, so domain changes (new fields, renamed rules) cannot leak
 * into the UI and the view cannot format anything on its own.
 *
 * `subtitle`/`explanation` are styled: the feed ships HTML fragments and Jsoup
 * renders `<strong>`/`<a href>` into spans. Titles stay plain.
 */
data class PlanetUi(
    val date: String,
    val title: String,
    val subtitle: AnnotatedString,
    val explanation: AnnotatedString,
    val imageUrl: String?,
    val imageContentDescription: String,
    val permalink: String?,
    val isVideo: Boolean,
) {
    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()
}

fun Planet.toUi(): PlanetUi = PlanetUi(
    date = date,
    title = displayTitle,
    subtitle = buildAnnotatedString {
        append(date)
        val author = credit
        if (!author.isNullOrBlank()) {
            append(" • ")
            append(author.toRichText())
        }
    },
    explanation = explanation.toRichText(),
    imageUrl = imageUrl,
    imageContentDescription = imageContentDescription,
    permalink = permalink,
    isVideo = isVideo,
)