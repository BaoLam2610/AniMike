package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP6 Tracking local — trạng thái xem của user cho 1 anime (Room, không có
// API). Thứ tự khai báo = thứ tự hiển thị chip chọn trạng thái ở Detail.
enum class WatchStatus {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DROPPED,
    PLAN_TO_WATCH,
}

// 1 dòng tracking đầy đủ của user cho 1 anime — status (đợt 1) +
// episodesWatched/personalScore (đợt 2, đã có sẵn cột trong Room để không
// phải bump schema 2 lần). Mọi field tracking đều nullable — row bị xoá khi
// không còn dữ liệu nào (xem TrackingDao.toggleStatus).
@Immutable
data class TrackedAnime(
    val anime: Anime,
    val status: WatchStatus?,
    val episodesWatched: Int?,
    val personalScore: Int?,
)
