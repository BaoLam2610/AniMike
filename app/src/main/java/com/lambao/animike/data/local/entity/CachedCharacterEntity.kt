package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache cho danh sách nhân vật (/characters) ở Detail — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_character", primaryKeys = ["malId", "characterId"])
data class CachedCharacterEntity(
    val malId: Int,
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val role: String,
    val voiceActorName: String?,
    val position: Int,
    val fetchedAt: Long,
)
