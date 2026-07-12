package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.TrackingEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.TrackedAnime
import com.lambao.animike.domain.model.WatchStatus

fun TrackingEntity.toDomain(): TrackedAnime = TrackedAnime(
    anime = Anime(malId = malId, title = title, imageUrl = imageUrl, score = score, year = year),
    // runCatching: phòng giá trị status cũ không còn khớp enum sau khi đổi
    // tên/thêm bớt — coi như chưa set thay vì crash (cùng kỹ thuật ReviewTag
    // ở CachedReviewPreviewMapper).
    status = status?.let { raw -> runCatching { WatchStatus.valueOf(raw) }.getOrNull() },
    episodesWatched = episodesWatched,
    personalScore = personalScore,
)

fun Anime.toTrackingEntity(status: WatchStatus, updatedAt: Long): TrackingEntity = TrackingEntity(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    score = score,
    year = year,
    status = status.name,
    episodesWatched = null,
    personalScore = null,
    updatedAt = updatedAt,
)

// MVP6 Đợt 2 — snapshot KHÔNG mang status/episodesWatched/personalScore, chỉ
// chở title/ảnh/score/year mới nhất + updatedAt. Dùng làm "candidate" cho
// TrackingDao.updateEpisodesWatched/updatePersonalScore — 2 hàm đó tự merge
// field cần đổi với row hiện có (nếu có), giữ nguyên 2 field tracking còn lại
// (khác toTrackingEntity ở trên vốn dành riêng cho toggleStatus).
fun Anime.toTrackingSnapshot(updatedAt: Long): TrackingEntity = TrackingEntity(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    score = score,
    year = year,
    status = null,
    episodesWatched = null,
    personalScore = null,
    updatedAt = updatedAt,
)
