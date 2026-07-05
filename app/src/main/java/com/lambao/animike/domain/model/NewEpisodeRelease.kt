package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** "Tập mới phát hành" trên Home (MVP4, /watch/episodes) — không có score/year. */
@Immutable
data class NewEpisodeRelease(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val episodeLabel: String,
)
