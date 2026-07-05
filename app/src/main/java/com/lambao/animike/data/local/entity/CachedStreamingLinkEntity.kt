package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache nút "Xem trên..." (MVP4, /anime/{id}/streaming) — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_streaming_link", primaryKeys = ["malId", "url"])
data class CachedStreamingLinkEntity(
    val malId: Int,
    val url: String,
    val name: String,
    val position: Int,
    val fetchedAt: Long,
)
