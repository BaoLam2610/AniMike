package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AnimeDetail(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val trailerYoutubeId: String?,
    val score: String,
    val rank: String,
    val type: String?,
    val episodes: Int?,
    val year: Int?,
    val status: String,
    val isAiring: Boolean,
    // MVP5 — list (không phải String join sẵn) để bấm từng studio mở Studio
    // Detail; rỗng khi Jikan không trả studio (thay cho "N/A" cũ, UI tự ẩn).
    val studios: List<Studio>,
    val genres: List<String>,
    val synopsis: String,
    val relations: List<RelationGroup>,
) {
    // Thumbnail trailer derive từ video id theo pattern công khai của YouTube
    // (hqdefault luôn tồn tại; 4:3 có thể kèm letterbox — UI crop khung 16:9
    // sẽ tự cắt bỏ bar đen). Derive thay vì lưu trailer.images từ API để khỏi
    // thêm cột vào Room cache (CachedAnimeDetailEntity) chỉ vì 1 URL suy ra được.
    val trailerThumbnailUrl: String?
        get() = trailerYoutubeId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}

@Immutable
data class RelationGroup(
    val relation: String,
    val titles: List<String>,
)

// MVP5 Studio Detail — chỉ cần malId (mở /producers/{id}/full) + name (hiển
// thị chip ở Detail). Jikan gọi chung "producers" nhưng field ở anime là
// `studios`, giữ tên Studio cho khớp ngữ cảnh anime.
@Immutable
data class Studio(
    val malId: Int,
    val name: String,
)
