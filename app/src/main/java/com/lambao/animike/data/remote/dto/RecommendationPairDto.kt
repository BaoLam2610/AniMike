package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /recommendations/anime (MVP4 "Đề xuất cộng đồng") — feed toàn cục ghép cặp
// 2 anime kèm lý do do user MAL viết, KHÁC RecommendationEntryDto của
// /anime/{id}/recommendations (chỉ 1 entry vì đã có context anime hiện tại).
// Verify qua curl: entry LUÔN có đúng 2 phần tử, mal_id là chuỗi ghép dạng
// "30-51552" (không phải Int như các list khác), phân trang THẬT (100
// item/trang, has_next_page đổi qua từng trang — khác /watch/episodes*).
@Serializable
data class RecommendationPairDto(
    @SerialName("mal_id") val malId: String? = null,
    val entry: List<RecommendationPairAnimeDto> = emptyList(),
    val content: String? = null,
    val user: RecommendationPairUserDto? = null,
)

@Serializable
data class RecommendationPairAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val title: String? = null,
)

@Serializable
data class RecommendationPairUserDto(
    val username: String? = null,
)
