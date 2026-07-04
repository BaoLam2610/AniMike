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
}
