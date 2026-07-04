package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Từ /anime/{id}/videos/episodes (KHÔNG phải /anime/{id}/episodes) — endpoint
// này có thumbnail và trả sẵn thứ tự mới nhất trước, đúng thứ tự muốn hiển thị.
// Đánh đổi: chỉ chứa tập có video/promo — anime dài (VD One Piece 1000+ tập)
// có thể thiếu vài chục tập đầu quá cũ, nhưng đa số anime hiện đại đủ 100%.
@Serializable
data class EpisodeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    val images: ImagesDto? = null,
)
