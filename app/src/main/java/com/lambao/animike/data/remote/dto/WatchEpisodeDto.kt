package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /watch/episodes: mỗi entry là 1 anime kèm danh sách tập MỚI vừa ra (có thể
// nhiều tập/anime) — chỉ lấy tập đầu (mới nhất) để hiển thị 1 nhãn "Episode N"
// như kit Animax. Không có field score/year (khác /anime list thường).
@Serializable
data class WatchEpisodeEntryDto(
    val entry: WatchAnimeEntryDto? = null,
    val episodes: List<WatchEpisodeItemDto> = emptyList(),
)

@Serializable
data class WatchAnimeEntryDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    val images: ImagesDto? = null,
)

@Serializable
data class WatchEpisodeItemDto(
    val title: String? = null,
)
