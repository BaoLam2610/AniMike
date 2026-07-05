package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache preview "Đề xuất cộng đồng" trên Home (MVP4, /recommendations/anime) — 1 feed toàn cục, không cần listKey. */
@Entity(tableName = "cached_community_recommendation", primaryKeys = ["id"])
data class CachedCommunityRecommendationEntity(
    val id: String,
    val firstAnimeId: Int,
    val firstAnimeTitle: String,
    val firstAnimeImageUrl: String?,
    val secondAnimeId: Int,
    val secondAnimeTitle: String,
    val secondAnimeImageUrl: String?,
    val content: String,
    val username: String,
    val position: Int,
    val fetchedAt: Long,
)
