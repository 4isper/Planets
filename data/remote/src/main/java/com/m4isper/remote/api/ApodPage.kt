package com.m4isper.remote.api

import com.m4isper.remote.model.ApodResponse
import retrofit2.HttpException
import retrofit2.Response

/** The science.nasa.gov REST API silently caps `per_page` at [MAX_PAGE_SIZE]. */
const val MAX_PAGE_SIZE = 25

data class ApodPage(
    val items: List<ApodResponse>,
    val totalPages: Int,
)

suspend fun NasaApiService.getApodPage(
    page: Int,
    perPage: Int = MAX_PAGE_SIZE,
): ApodPage {
    val response: Response<List<ApodResponse>> = getApodList(page = page, perPage = perPage)
    val items = response.body() ?: throw HttpException(response)
    val totalPages = response.headers()[TOTAL_PAGES_HEADER]?.toIntOrNull() ?: Int.MAX_VALUE
    return ApodPage(items = items, totalPages = totalPages)
}

private const val TOTAL_PAGES_HEADER = "X-WP-TotalPages"