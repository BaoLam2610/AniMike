package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// userAvatarUrl/date/tag/reactions: null khi nguồn dữ liệu không mang theo
// (VD preview cache ở Detail — cached_review_preview chỉ lưu username/score/
// reviewText, xem CachedReviewPreviewMapper) — chỉ ReviewsScreen (Paging,
// live /anime/{id}/reviews) mới có đủ.
@Immutable
data class AnimeReview(
    val id: Int,
    val username: String,
    val userAvatarUrl: String?,
    val score: Int?,
    val reviewText: String,
    val date: String?,
    val tag: ReviewTag?,
    val reactions: ReviewReactions?,
)

// 3 giá trị (đã verify qua curl — không phải 2 như giả định ban đầu), MAL
// dùng để phân loại review theo mức khuyến nghị.
enum class ReviewTag { RECOMMENDED, MIXED_FEELINGS, NOT_RECOMMENDED }

@Immutable
data class ReviewReactions(
    val overall: Int,
    val nice: Int,
    val loveIt: Int,
    val funny: Int,
    val confusing: Int,
    val informative: Int,
    val wellWritten: Int,
    val creative: Int,
)
