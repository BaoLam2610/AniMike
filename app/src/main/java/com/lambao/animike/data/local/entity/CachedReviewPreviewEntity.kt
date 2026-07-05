package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache cho preview "Đánh giá" (page 1, top 5) ở Detail — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_review_preview", primaryKeys = ["malId", "reviewId"])
data class CachedReviewPreviewEntity(
    val malId: Int,
    val reviewId: Int,
    val username: String,
    val score: Int?,
    val reviewText: String,
    val position: Int,
    val fetchedAt: Long,
)
