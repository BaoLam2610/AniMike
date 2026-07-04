package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    @SerialName("title_english") val titleEnglish: String? = null,
    val images: ImagesDto? = null,
    val score: Double? = null,
    val year: Int? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val broadcast: BroadcastDto? = null,
)

@Serializable
data class ImagesDto(
    val jpg: ImageUrlsDto? = null,
)

// Field có mặt trong response của mọi endpoint list (không riêng /schedules)
// vì cùng 1 anime resource — hiện chỉ ScheduledAnimeMapper đọc, mapper khác
// (AnimeMapper, RecommendationMapper...) chủ động bỏ qua vì không cần.
@Serializable
data class BroadcastDto(
    val time: String? = null,
)

@Serializable
data class ImageUrlsDto(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)
