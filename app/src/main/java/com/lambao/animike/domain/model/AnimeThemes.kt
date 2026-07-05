package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** "Nhạc OP/ED" (MVP4, /anime/{id}/themes) — mỗi chuỗi đã format sẵn từ Jikan. */
@Immutable
data class AnimeThemes(
    val openings: List<String>,
    val endings: List<String>,
)
