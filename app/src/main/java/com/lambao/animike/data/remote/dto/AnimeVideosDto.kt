package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /anime/{id}/videos — verify qua curl (anime 1 + 20): data là 1 OBJECT gồm
// 3 mảng. `episodes` CỐ Ý bỏ qua không khai — trùng hoàn toàn với
// /anime/{id}/videos/episodes đã dùng cho EpisodesSection (có Paging riêng).
// promo dùng field `trailer`, music_videos dùng field `video` — cả 2 cùng
// shape TrailerDto (youtube_id/url/embed_url); youtube_id thường NULL chỉ có
// embed_url (cùng data quirk với trailer ở /anime/{id}/full — xem
// resolveYoutubeId), images cũng luôn null nên thumbnail phải derive từ id.
@Serializable
data class AnimeVideosDto(
    val promo: List<PromoVideoDto> = emptyList(),
    @SerialName("music_videos") val musicVideos: List<MusicVideoDto> = emptyList(),
)

@Serializable
data class PromoVideoDto(
    val title: String? = null,
    val trailer: TrailerDto? = null,
)

@Serializable
data class MusicVideoDto(
    val title: String? = null,
    val video: TrailerDto? = null,
    val meta: MusicVideoMetaDto? = null,
)

@Serializable
data class MusicVideoMetaDto(
    val title: String? = null,
    val author: String? = null,
)
