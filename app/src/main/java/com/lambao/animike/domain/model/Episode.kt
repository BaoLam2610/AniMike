package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Episode(
    // mal_id của Jikan trong /videos/episodes chính là số thứ tự tập (1, 2, 3...).
    val number: Int,
    val title: String,
    val imageUrl: String?,
)
