package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.domain.model.AnimeReview

fun CachedReviewPreviewEntity.toDomain(): AnimeReview = AnimeReview(
    id = reviewId,
    username = username,
    score = score,
    reviewText = reviewText,
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
