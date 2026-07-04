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
    // Năm phát hành (kit Animax MVP3 UI-6) — map sang start_date/end_date của
    // năm đó, Jikan không có tham số "year" riêng cho /anime search.
    val year: Int? = null,
)
