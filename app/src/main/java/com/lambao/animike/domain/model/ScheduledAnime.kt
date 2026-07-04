package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// Model riêng cho màn Lịch chiếu (tab Duyệt) — không dùng chung Anime vì cần
// episodes/broadcastTime mà các màn khác (Search/SeasonArchive/Home/Favorites)
// không cần, tránh phình field + Room migration không liên quan. Vẫn giữ
// score/year (dù Schedules không hiển thị) để khi toggle favorite từ đây,
// Anime lưu vào Room không bị mất dữ liệu — xem ScheduledAnimeMapper.
@Immutable
data class ScheduledAnime(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
    val episodes: Int?,
    val broadcastTime: String?,
)
