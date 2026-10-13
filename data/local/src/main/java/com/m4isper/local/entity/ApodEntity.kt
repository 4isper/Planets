package com.m4isper.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "apod",
    indices = [Index(value = ["page", "position"])],
)
data class ApodEntity(
    @PrimaryKey
    val date: String,
    val title: String?,
    val explanation: String?,
    val mediaType: String?,
    val credit: String?,
    val alt: String?,
    val permalink: String?,
    val imageUrl: String?,
    val page: Int,
    val position: Int,
)