package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Anime(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
)
