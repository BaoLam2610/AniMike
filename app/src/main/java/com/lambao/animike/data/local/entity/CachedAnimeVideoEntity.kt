package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache tab "Video" (MVP4, /anime/{id}/videos: promo + music video) — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_anime_video", primaryKeys = ["malId", "youtubeId"])
data class CachedAnimeVideoEntity(
    val malId: Int,
    val youtubeId: String,
    val title: String,
    val subtitle: String?,
    val position: Int,
    val fetchedAt: Long,
)
