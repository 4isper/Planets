package com.m4isper.planets.ui.model

import com.m4isper.domain.model.Planet

/**
 * Everything a screen needs to render a record.
 *
 * Composables never see [Planet]: the domain model stays an implementation detail of
 * the presentation layer, so domain changes (new fields, renamed rules) cannot leak
 * into the UI and the view cannot format anything on its own.
 */
data class PlanetUi(
    val date: String,
    val title: String,
    val subtitle: String,
    val explanation: String,
    val imageUrl: String?,
    val imageContentDescription: String,
    val permalink: String?,
    val isVideo: Boolean,
) {
    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()

    val openLabel: String
        get() = if (isVideo) "Смотреть видео" else "Открыть оригинал"
}

fun Planet.toUi(): PlanetUi = PlanetUi(
    date = date,
    title = displayTitle,
    subtitle = listOfNotNull(date, credit).joinToString(" • "),
    explanation = explanation,
    imageUrl = imageUrl,
    imageContentDescription = imageContentDescription,
    permalink = permalink,
    isVideo = isVideo,
)