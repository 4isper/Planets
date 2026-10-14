package com.m4isper.remote.api

import com.m4isper.remote.model.ApodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NasaApiService {

    /**
     * Pages of the archive, newest first.
     *
     * The order is not negotiable: `orderby` and `order` are not registered query
     * vars here and are ignored, so date-descending is the endpoint's own default and
     * the paging keys depend on it. Asking for a different order would look like it
     * worked and quietly return the same pages.
     */
    @GET("wp-json/wp/v2/apod-basic")
    suspend fun getApodList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("_fields") fields: String = REQUESTED_FIELDS,
    ): Response<List<ApodResponse>>

    /**
     * Single record, used to recover a details screen whose cache entry is gone.
     *
     * The date is a six digit `YYMMDD` path segment. The list endpoint takes no
     * filter for a single day: `date`, `slug`, `apod_date` and `search` are not
     * registered query vars and are silently ignored, and the `date_from`/`date_to`
     * pair the endpoint advertises answers `400`. This route is the only way in.
     */
    @GET("wp-json/wp/v2/apod-basic/{date}")
    suspend fun getApod(
        @Path("date") date: String,
        @Query("_fields") fields: String = REQUESTED_FIELDS,
    ): Response<ApodResponse>
}

/**
 * Without `_fields` the feed embeds a full HTML page (`basic_html`) per record,
 * which inflates a 25-item page from ~75 KB to ~290 KB.
 */
private const val REQUESTED_FIELDS =
    "date,post_id,title,permalink,media_type,explanation,credit,alt,url,hdurl"