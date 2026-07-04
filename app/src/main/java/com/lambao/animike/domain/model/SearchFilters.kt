package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** order_by/sort theo quy ước Jikan (.claude/skills/jikan-api). */
@Immutable
data class SearchFilters(
    val type: String? = null, // TV, Movie, OVA, Special, ONA, Music
    val status: String? = null, // airing, complete, upcoming
    val genreIds: Set<Int> = emptySet(),
    val orderBy: String = "popularity",
    val sort: String = "asc",
)
