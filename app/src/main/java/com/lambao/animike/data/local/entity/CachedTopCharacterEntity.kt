package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache preview "Nhân vật nổi bật" trên Home (MVP5, /top/characters page 1) — 1 feed toàn cục, giống cached_new_episode. */
@Entity(tableName = "cached_top_character", primaryKeys = ["malId"])
data class CachedTopCharacterEntity(
    val malId: Int,
    val name: String,
    val imageUrl: String?,
    val favorites: Int,
    val position: Int,
    val fetchedAt: Long,
)
