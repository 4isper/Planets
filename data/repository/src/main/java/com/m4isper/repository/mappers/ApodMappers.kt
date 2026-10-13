package com.m4isper.repository.mappers

import com.m4isper.domain.model.Planet
import com.m4isper.local.entity.ApodEntity
import com.m4isper.remote.model.ApodResponse

fun ApodResponse.toEntity(page: Int, position: Int): ApodEntity? {
    val date = date ?: return null
    return ApodEntity(
        date = date,
        title = title.stripHtml(),
        explanation = explanation.stripApodHtml(),
        mediaType = mediaType,
        credit = credit.stripHtml(),
        alt = alt.stripHtml().ifEmpty { null },
        permalink = permalink ?: url,
        imageUrl = hdUrl ?: url,
        page = page,
        position = position,
    )
}

fun ApodEntity.toDomain(): Planet = Planet(
    date = date,
    title = title.orEmpty(),
    explanation = explanation.orEmpty(),
    mediaType = mediaType.orEmpty(),
    credit = credit?.ifEmpty { null },
    imageDescription = alt?.ifEmpty { null },
    permalink = permalink,
    imageUrl = imageUrl,
)