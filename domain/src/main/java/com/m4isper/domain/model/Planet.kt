package com.m4isper.domain.model

data class Planet(
    val date: String,
    /** Plain text; titles carry no links. */
    val title: String,
    /** Raw HTML fragment (`<strong>`, `<a>`, entities); the app renders it rich. */
    val explanation: String,
    val mediaType: String,
    /** Raw HTML fragment; null when the feed gave neither credit nor copyright. */
    val credit: String?,
    val imageDescription: String?,
    val permalink: String?,
    val imageUrl: String?,
) {
    val isVideo: Boolean
        get() = mediaType == "video" || mediaType == "iframe"

    /** APOD records sometimes carry no title; the date is a usable fallback. */
    val displayTitle: String
        get() = title.ifBlank { date }

    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()

    /** APOD ships an `alt` description; fall back to the title when it is missing. */
    val imageContentDescription: String
        get() = imageDescription?.ifBlank { null } ?: displayTitle

    /** A record without an image or a title cannot be rendered as a card. */
    val isDisplayable: Boolean
        get() = hasImage && displayTitle.isNotBlank()
}