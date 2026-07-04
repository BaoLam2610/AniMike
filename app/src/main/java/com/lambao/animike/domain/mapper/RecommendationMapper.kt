package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.RecommendationEntryDto
import com.lambao.animike.domain.model.Anime

fun RecommendationEntryDto.toDomain(): Anime? {
    val e = entry ?: return null
    // /recommendations không trả score/year (không phải dữ liệu thiếu ngẫu
    // nhiên mà do cấu trúc response cố định) — tái dùng Anime để dùng lại
    // AnimeCard, score cố định "N/A"/year null theo đúng quy ước hiển thị.
    return Anime(
        malId = e.malId,
        title = e.title ?: "Không rõ tên",
        imageUrl = e.images?.jpg?.largeImageUrl ?: e.images?.jpg?.imageUrl,
        score = "N/A",
        year = null,
    )
}
