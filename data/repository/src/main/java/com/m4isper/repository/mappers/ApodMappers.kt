package com.m4isper.repository.mappers

import com.m4isper.domain.model.Planet
import com.m4isper.local.entity.ApodEntity
import com.m4isper.remote.model.ApodResponse

fun ApodResponse.toEntity(page: Int, position: Int): ApodEntity? {
    val date = date ?: return null
    // No DB migration: `copyright` is merged into the existing `credit` column.
    // `explanation`/`credit` keep raw HTML (boilerplate cut, tags kept);
    // the app renders them with Jsoup -> AnnotatedString. Plain rows cached
    // by older versions have no tags and parse to the same text.
    val authorRaw = credit?.ifBlank { null } ?: copyright
    val author = authorRaw?.trim().orEmpty().ifEmpty { null }
    // `url` matches `permalink` (article page); only images may fall back to it.
    // Video/iframe previews live in `hdurl`, otherwise there is no image.
    val preview = if (mediaType == "video" || mediaType == "iframe") hdUrl else hdUrl ?: url
    return ApodEntity(
        date = date,
        title = title.stripHtml(),
        explanation = explanation.cutApodBoilerplateHtml().ifEmpty { null },
        mediaType = mediaType,
        credit = author,
        alt = alt.stripHtml().ifEmpty { null },
        permalink = permalink ?: url,
        imageUrl = preview,
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