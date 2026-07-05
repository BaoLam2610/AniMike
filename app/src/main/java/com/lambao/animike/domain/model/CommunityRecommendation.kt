package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** "Đề xuất cộng đồng" (MVP4, /recommendations/anime) — ghép cặp 2 anime kèm lý do user MAL viết. */
@Immutable
data class CommunityRecommendation(
    val id: String,
    val firstAnimeId: Int,
    val firstAnimeTitle: String,
    val firstAnimeImageUrl: String?,
    val secondAnimeId: Int,
    val secondAnimeTitle: String,
    val secondAnimeImageUrl: String?,
    val content: String,
    val username: String,
)
