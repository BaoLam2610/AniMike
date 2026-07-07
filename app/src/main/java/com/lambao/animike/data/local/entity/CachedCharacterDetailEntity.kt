package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache Character Detail (MVP5, /characters/{id}/full) — 1 row/nhân vật, giống cached_anime_detail. */
@Entity(tableName = "cached_character_detail")
data class CachedCharacterDetailEntity(
    @PrimaryKey val characterId: Int,
    val name: String,
    val nameKanji: String?,
    val imageUrl: String?,
    // Encode bằng delimiter ASCII (xem CachedCharacterDetailMapper.kt) — cùng
    // kỹ thuật genresEncoded của CachedAnimeDetailEntity.
    val nicknamesEncoded: String,
    val favorites: Int,
    val about: String?,
    val fetchedAt: Long,
)
