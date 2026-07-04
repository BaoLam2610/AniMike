package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_season_year")
data class CachedSeasonYearEntity(
    @PrimaryKey val year: Int,
    // "winter,spring,summer,fall" — vocab cố định nhỏ, dấu phẩy an toàn tuyệt
    // đối làm delimiter (khác với text tự do như tên anime).
    val seasonsCsv: String,
    val fetchedAt: Long,
)
