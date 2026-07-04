package com.lambao.animike.ui.search

import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters

data class SearchState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val genres: List<Genre> = emptyList(),
)

sealed interface SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent
    data class OnTypeFilterChange(val type: String?) : SearchEvent
    data class OnStatusFilterChange(val status: String?) : SearchEvent
    data class OnGenreToggle(val genreId: Int) : SearchEvent
    data class OnSortChange(val orderBy: String, val sort: String) : SearchEvent
    data class OnAnimeClick(val malId: Int) : SearchEvent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val malId: Int) : SearchEffect
}
