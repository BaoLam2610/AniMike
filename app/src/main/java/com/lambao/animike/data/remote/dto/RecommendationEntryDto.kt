package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendationEntryDto(
    val entry: RecommendationAnimeDto? = null,
)

@Serializable
data class RecommendationAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    val images: ImagesDto? = null,
)
