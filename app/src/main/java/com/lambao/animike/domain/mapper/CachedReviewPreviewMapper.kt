package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.domain.model.AnimeReview

// userAvatarUrl/date/tag/reactions: null — cache preview (top 5 ở Detail)
// chỉ lưu username/score/reviewText, KHÔNG mở rộng schema chỉ để chứa dữ
// liệu mà ReviewCard preview ở Detail không hiển thị (avatar/tag/reactions
// đầy đủ chỉ có ở ReviewsScreen — Paging, live /anime/{id}/reviews).
fun CachedReviewPreviewEntity.toDomain(): AnimeReview = AnimeReview(
    id = reviewId,
    username = username,
    userAvatarUrl = null,
    score = score,
    reviewText = reviewText,
    date = null,
    tag = null,
    reactions = null,
)

fun AnimeReview.toPreviewEntity(malId: Int, position: Int, fetchedAt: Long): CachedReviewPreviewEntity =
    CachedReviewPreviewEntity(
        malId = malId,
        reviewId = id,
        username = username,
        score = score,
        reviewText = reviewText,
        position = position,
        fetchedAt = fetchedAt,
    )
