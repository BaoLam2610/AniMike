package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_anime_detail")
data class CachedAnimeDetailEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String?,
    val trailerYoutubeId: String?,
    val score: String,
    val rank: String,
    val type: String?,
    val episodes: Int?,
    val year: Int?,
    val status: String,
    val isAiring: Boolean,
    val studios: String,
    // Danh sách encode bằng delimiter ASCII (xem CachedAnimeDetailMapper.kt) —
    // tránh phải thêm TypeConverter + @Serializable vào domain model chỉ để
    // lưu vài chuỗi hiển thị.
    val genresEncoded: String,
    val synopsis: String,
    val relationsEncoded: String,
    val fetchedAt: Long,
)
