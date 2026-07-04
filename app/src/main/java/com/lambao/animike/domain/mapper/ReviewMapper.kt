package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.ReviewDto
import com.lambao.animike.domain.model.AnimeReview

fun ReviewDto.toDomain(): AnimeReview = AnimeReview(
    id = malId,
    username = user?.username ?: "Ẩn danh",
    score = score,
    reviewText = review ?: "",
)
