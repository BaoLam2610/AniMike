package com.lambao.animike.ui.search

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters

@Immutable
data class SearchState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val genres: List<Genre> = emptyList(),
)

sealed interface SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent
    // Áp dụng nguyên cụm filter 1 lần từ màn "Sắp xếp & Lọc" (kit Animax MVP3
    // UI-6, nút Apply) — thay cho các event sửa từng field riêng lẻ trước đây,
    // vì màn filter giờ có draft cục bộ + Apply/Reset, không áp ngay từng tap.
    data class OnFiltersApplied(val filters: SearchFilters) : SearchEvent
    data class OnAnimeClick(val malId: Int) : SearchEvent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val malId: Int) : SearchEffect
}
