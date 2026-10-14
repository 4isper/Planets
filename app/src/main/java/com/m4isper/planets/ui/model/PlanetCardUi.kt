package com.m4isper.planets.ui.model

import com.m4isper.domain.model.Planet

/**
 * What the grid tile needs: title, image and the media flag.
 *
 * Deliberately free of rich text: the card never renders explanation or credit
 * links, so mapping it pays no Jsoup cost and stays trivially main-safe no
 * matter how large the cached feed grows. Full rendering lives in [PlanetUi].
 */
data class PlanetCardUi(
    val date: String,
    val title: String,
    val imageUrl: String?,
    val imageContentDescription: String,
    val isVideo: Boolean,
) {
    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()
}

fun Planet.toCardUi(): PlanetCardUi = PlanetCardUi(
    date = date,
    title = displayTitle,
    imageUrl = imageUrl,
    imageContentDescription = imageContentDescription,
    isVideo = isVideo,
)
