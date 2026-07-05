package com.lambao.animike.data.remote.dto

import kotlinx.serialization.Serializable

// /anime/{id}/themes — mỗi phần tử là 1 chuỗi đã format sẵn từ Jikan
// (VD: "\"Tank!\" by The Seatbelts (eps 1-25)"), không có field con.
@Serializable
data class AnimeThemesDto(
    val openings: List<String> = emptyList(),
    val endings: List<String> = emptyList(),
)
