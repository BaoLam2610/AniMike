package com.lambao.animike.domain.model

data class Anime(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
)
