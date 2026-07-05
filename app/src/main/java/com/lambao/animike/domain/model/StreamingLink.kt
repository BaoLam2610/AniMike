package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** Nút "Xem trên..." (MVP4, /anime/{id}/streaming) — nền tảng streaming hợp pháp. */
@Immutable
data class StreamingLink(
    val name: String,
    val url: String,
)
