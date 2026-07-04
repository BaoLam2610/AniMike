package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SeasonYear(
    val year: Int,
    val seasons: List<String>, // "winter", "spring", "summer", "fall"
)
