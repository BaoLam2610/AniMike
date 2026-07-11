package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MVP6 Tracking local — 1 row/anime user đang theo dõi. Lưu snapshot
 * title/imageUrl/score/year (giống FavoriteEntity) để màn "Danh sách" hiển
 * thị được mà không cần gọi API/join bảng cache. status lưu WatchStatus.name;
 * episodesWatched/personalScore là cột ĐỢT 2 (khai sẵn để không bump schema
 * 2 lần) — cả 3 đều nullable, row bị xoá khi cả 3 null (TrackingDao).
 */
@Entity(tableName = "tracking")
data class TrackingEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
    val status: String?,
    val episodesWatched: Int?,
    val personalScore: Int?,
    val updatedAt: Long,
)
