package com.m4isper.remote.api

import com.m4isper.remote.model.ApodResponse
import retrofit2.HttpException
import retrofit2.Response

private const val HTTP_NOT_FOUND = 404

/**
 * One record by date, or `null` when the archive carries no APOD for it.
 *
 * A date the feed does not cover answers `404 apod_basic_not_found`. That is an
 * answer, not a transport failure: it must not reach the UI as a retryable error,
 * because retrying it can never succeed.
 *
 * @throws retrofit2.HttpException on any other non-success status.
 */
suspend fun NasaApiService.getApodRecord(date: String): ApodResponse? {
    val response: Response<ApodResponse> = getApod(date = date.toApodPathSegment())
    if (response.code() == HTTP_NOT_FOUND) return null
    return response.body() ?: throw HttpException(response)
}

/**
 * `2026-10-12` -> `261012`, the `YYMMDD` path segment the record route is keyed on.
 *
 * The domain and the cache both key on `yyyy-MM-dd`, so the conversion belongs to
 * the wire format and stays here rather than leaking into either.
 */
internal fun String.toApodPathSegment(): String {
    val date = APOD_DATE.matchEntire(this)
        ?: throw IllegalArgumentException("APOD date must be yyyy-MM-dd, was '$this'")
    val (year, month, day) = date.destructured
    return year.takeLast(SHORT_YEAR_LENGTH) + month + day
}

private val APOD_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")

private const val SHORT_YEAR_LENGTH = 2