package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /anime/{id}/statistics — verify qua curl: LUÔN đúng 10 phần tử trong
// `scores` (score 1-10), field top-level không nullable trên response thật
// nhưng vẫn khai nullable để phòng anime hiếm/mới không có đủ dữ liệu MAL.
@Serializable
data class AnimeStatisticsDto(
    val watching: Int? = null,
    val completed: Int? = null,
    @SerialName("on_hold") val onHold: Int? = null,
    val dropped: Int? = null,
    @SerialName("plan_to_watch") val planToWatch: Int? = null,
    val total: Int? = null,
    val scores: List<ScoreStatDto> = emptyList(),
)

@Serializable
data class ScoreStatDto(
    // Nullable như mọi field khác (jikan-api SKILL.md: "mọi field nullable
    // trừ mal_id") — dù response thật luôn có score, không loại trừ khả năng
    // Jikan trả entry thiếu score ở vài response lẻ.
    val score: Int? = null,
    val votes: Int? = null,
    val percentage: Double? = null,
)
