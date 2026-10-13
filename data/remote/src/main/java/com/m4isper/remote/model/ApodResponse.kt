package com.m4isper.remote.model

import com.google.gson.annotations.SerializedName

data class ApodResponse(
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("post_id")
    val postId: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("permalink")
    val permalink: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = null,
    @SerializedName("explanation")
    val explanation: String? = null,
    @SerializedName("credit")
    val credit: String? = null,
    @SerializedName("copyright")
    val copyright: String? = null,
    @SerializedName("alt")
    val alt: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("hdurl")
    val hdUrl: String? = null,
)