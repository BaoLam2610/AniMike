package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache "Nhạc OP/ED" (MVP4, /anime/{id}/themes) — 1 row/anime, giống cached_anime_detail. */
@Entity(tableName = "cached_anime_themes")
data class CachedAnimeThemesEntity(
    @PrimaryKey val malId: Int,
    // Encode bằng delimiter ASCII (xem CachedAnimeThemesMapper.kt) — cùng kỹ
    // thuật với CachedAnimeDetailEntity.genresEncoded.
    val openingsEncoded: String,
    val endingsEncoded: String,
    val fetchedAt: Long,
)
