package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("mal_id") val malId: Int,
    val review: String? = null,
    val score: Int? = null,
    val user: ReviewUserDto? = null,
)

@Serializable
data class ReviewUserDto(
    val username: String? = null,
)
