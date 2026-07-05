package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache cho Home sections (Season Now/Top/Upcoming) — `listKey` phân biệt section. */
@Entity(tableName = "cached_anime_list", primaryKeys = ["listKey", "malId"])
data class CachedAnimeListEntity(
    val listKey: String,
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
    val position: Int,
    val fetchedAt: Long,
)

object AnimeListKey {
    const val SEASON_NOW = "season_now"
    const val TOP_ANIME = "top_anime"
    const val UPCOMING = "upcoming"

    // Tái dùng bảng cached_anime_list cho Recommendations ở Detail thay vì
    // thêm entity/DAO riêng — shape (malId, title, imageUrl, score, year) đã
    // khớp Anime. Key theo malId của anime cha để mỗi Detail có bucket riêng,
    // primary key (listKey, malId) đã đủ tránh đụng độ với 3 key ở trên.
    fun detailRecommendations(malId: Int) = "detail_recommendations_$malId"
}
