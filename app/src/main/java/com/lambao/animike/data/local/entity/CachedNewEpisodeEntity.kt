package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache cho "Tập mới phát hành" trên Home (MVP4, /watch/episodes) — 1 feed toàn cục, không cần listKey. */
@Entity(tableName = "cached_new_episode", primaryKeys = ["malId"])
data class CachedNewEpisodeEntity(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val episodeLabel: String,
    val position: Int,
    val fetchedAt: Long,
)
