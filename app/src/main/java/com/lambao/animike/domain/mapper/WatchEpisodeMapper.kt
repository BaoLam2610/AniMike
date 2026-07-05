package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.WatchEpisodeEntryDto
import com.lambao.animike.domain.model.NewEpisodeRelease

fun WatchEpisodeEntryDto.toDomain(): NewEpisodeRelease? {
    val e = entry ?: return null
    // Tập đầu tiên trong list là mới nhất (đã verify qua curl) — chỉ cần 1
    // nhãn hiển thị như kit ("Episode N"), bỏ qua các tập cũ hơn nếu có.
    val episodeLabel = episodes.firstOrNull()?.title ?: return null
    return NewEpisodeRelease(
        malId = e.malId,
        title = e.title ?: "Không rõ tên",
        imageUrl = e.images?.jpg?.largeImageUrl ?: e.images?.jpg?.imageUrl,
        episodeLabel = episodeLabel,
    )
}
