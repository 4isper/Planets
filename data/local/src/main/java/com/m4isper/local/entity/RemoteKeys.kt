package com.m4isper.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val id: Int = 0,
    val currentPage: Int,
    val totalPages: Int,
)