package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache "Biểu đồ phân bố điểm" (MVP4, /anime/{id}/statistics) — 1 row/anime, giống cached_anime_detail. */
@Entity(tableName = "cached_anime_statistics")
data class CachedAnimeStatisticsEntity(
    @PrimaryKey val malId: Int,
    val watching: Int,
    val completed: Int,
    val onHold: Int,
    val dropped: Int,
    val planToWatch: Int,
    val total: Int,
    // Danh sách 10 entry (score:votes:percentage) encode bằng delimiter ASCII
    // (xem CachedAnimeStatisticsMapper.kt) — cùng kỹ thuật với
    // CachedAnimeDetailEntity.genresEncoded, tránh thêm TypeConverter.
    val scoreDistributionEncoded: String,
    val fetchedAt: Long,
)
