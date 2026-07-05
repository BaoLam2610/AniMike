package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/**
 * 1 video trong tab "Video" của Detail (MVP4, /anime/{id}/videos) — gộp chung
 * promo/PV và music video vào 1 model: subtitle mang thông tin bài hát+nghệ sĩ
 * cho MV, null cho promo.
 */
@Immutable
data class AnimeVideo(
    val youtubeId: String,
    val title: String,
    val subtitle: String?,
) {
    // Derive từ youtubeId theo pattern công khai của YouTube — /videos trả
    // images LUÔN null (verify qua curl anime 1 + 20), cùng lý do với
    // AnimeDetail.trailerThumbnailUrl.
    val thumbnailUrl: String
        get() = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
}
