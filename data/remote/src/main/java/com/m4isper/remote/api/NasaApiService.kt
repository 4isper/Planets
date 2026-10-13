package com.m4isper.remote.api

import com.m4isper.remote.model.ApodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NasaApiService {

    @GET("wp-json/wp/v2/apod-basic")
    suspend fun getApodList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("orderby") orderBy: String = "date",
        @Query("order") order: String = "desc",
        @Query("_fields") fields: String = REQUESTED_FIELDS,
    ): Response<List<ApodResponse>>

    /** Single record, used to recover a details screen whose cache entry is gone. */
    @GET("wp-json/wp/v2/apod-basic")
    suspend fun getApod(
        @Query("date") date: String,
        @Query("_fields") fields: String = REQUESTED_FIELDS,
    ): Response<ApodResponse>
}

/**
 * Without `_fields` the feed embeds a full HTML page (`basic_html`) per record,
 * which inflates a 25-item page from ~75 KB to ~290 KB.
 */
private const val REQUESTED_FIELDS =
    "date,post_id,title,permalink,media_type,explanation,credit,alt,url,hdurl"