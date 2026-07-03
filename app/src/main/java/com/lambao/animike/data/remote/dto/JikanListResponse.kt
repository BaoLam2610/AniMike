package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanListResponse<T>(
    val data: List<T>,
    val pagination: PaginationDto? = null,
)

@Serializable
data class PaginationDto(
    @SerialName("last_visible_page") val lastVisiblePage: Int? = null,
    @SerialName("has_next_page") val hasNextPage: Boolean? = null,
    @SerialName("current_page") val currentPage: Int? = null,
)
