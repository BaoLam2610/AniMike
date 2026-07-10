package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache Studio Detail (MVP5, /producers/{id}/full) — 1 row/studio, giống cached_character_detail. */
@Entity(tableName = "cached_studio_detail")
data class CachedStudioDetailEntity(
    @PrimaryKey val studioId: Int,
    val name: String,
    val imageUrl: String?,
    val establishedYear: String?,
    val animeCount: Int,
    val favorites: Int,
    val about: String?,
    // Encode "name:::url" nối bằng ITEM_DELIMITER (xem CachedStudioDetailMapper.kt)
    // — cùng kỹ thuật studiosEncoded của CachedAnimeDetailEntity.
    val externalLinksEncoded: String,
    val fetchedAt: Long,
)
