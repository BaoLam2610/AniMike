package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeVideosDto
import com.lambao.animike.data.remote.dto.MusicVideoDto
import com.lambao.animike.data.remote.dto.PromoVideoDto
import com.lambao.animike.domain.model.AnimeVideo

// Gộp promo trước, music video sau (promo/PV liên quan trực tiếp tới phim
// hơn) thành 1 danh sách phẳng cho tab "Video". Item thiếu youtubeId (kể cả
// sau fallback rút từ embed_url — xem resolveYoutubeId) bị bỏ qua: không có
// id thì không mở được YouTube lẫn không derive được thumbnail.
fun AnimeVideosDto.toDomain(): List<AnimeVideo> =
    promo.mapNotNull { it.toDomain() } + musicVideos.mapNotNull { it.toDomain() }

private fun PromoVideoDto.toDomain(): AnimeVideo? {
    val youtubeId = trailer?.resolveYoutubeId() ?: return null
    return AnimeVideo(
        youtubeId = youtubeId,
        title = title?.takeIf { it.isNotBlank() } ?: "Promo",
        subtitle = null,
    )
}

private fun MusicVideoDto.toDomain(): AnimeVideo? {
    val youtubeId = video?.resolveYoutubeId() ?: return null
    // meta = tên bài hát + nghệ sĩ (VD "Haruka Kanata" — Asian Kung-fu
    // Generation) — ghép làm subtitle, phần nào thiếu thì bỏ phần đó.
    val subtitle = listOfNotNull(
        meta?.title?.takeIf { it.isNotBlank() },
        meta?.author?.takeIf { it.isNotBlank() },
    ).joinToString(" — ").ifBlank { null }
    return AnimeVideo(
        youtubeId = youtubeId,
        title = title?.takeIf { it.isNotBlank() } ?: "Music Video",
        subtitle = subtitle,
    )
}
