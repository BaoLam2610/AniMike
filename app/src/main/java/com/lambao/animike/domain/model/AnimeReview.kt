package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AnimeReview(
    val id: Int,
    val username: String,
    val score: Int?,
    val reviewText: String,
)
