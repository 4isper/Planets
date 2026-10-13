package com.m4isper.repository.mappers

import com.m4isper.domain.model.Pagination
import com.m4isper.local.entity.RemoteKeys

fun Pagination.toKeys(): RemoteKeys =
    RemoteKeys(currentPage = currentPage, totalPages = totalPages)

fun RemoteKeys.toPagination(): Pagination =
    Pagination(currentPage = currentPage, totalPages = totalPages)