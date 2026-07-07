package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache "Xuất hiện trong" ở Character Detail (MVP5, anime[] của /characters/{id}/full). */
@Entity(tableName = "cached_character_anime_appearance", primaryKeys = ["characterId", "animeMalId"])
data class CachedCharacterAnimeAppearanceEntity(
    val characterId: Int,
    val animeMalId: Int,
    val animeTitle: String,
    val animeImageUrl: String?,
    val role: String,
    val position: Int,
    val fetchedAt: Long,
)
