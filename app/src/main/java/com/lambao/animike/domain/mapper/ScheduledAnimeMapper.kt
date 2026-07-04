package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.domain.model.ScheduledAnime
import java.util.Locale

fun AnimeDto.toScheduledAnime(): ScheduledAnime = ScheduledAnime(
    malId = malId,
    title = titleEnglish ?: title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    // Cùng công thức với AnimeMapper.toDomain() — score/year không hiển thị ở
    // Schedules nhưng cần giữ đúng để favorite từ đây không mất dữ liệu.
    score = score?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
    year = year,
    episodes = episodes,
    broadcastTime = broadcast?.time,
)
