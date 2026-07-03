package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.domain.model.Anime
import java.util.Locale

fun AnimeDto.toDomain(): Anime = Anime(
    malId = malId,
    title = titleEnglish ?: title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    // Locale.US: tránh dấu phẩy thập phân trên thiết bị locale vi ("9,3" thay vì "9.3")
    score = score?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
    year = year,
)
