package com.lambao.animike.ui.animelist

// PagingData expose riêng qua AnimeListViewModel.items (không đặt trong state
// — xem compose-expert/references/paging-mvi-testing.md); source đọc 1 lần từ
// SavedStateHandle nên state không còn gì để giữ.
data object AnimeListState

sealed interface AnimeListEvent {
    data class OnAnimeClick(val malId: Int) : AnimeListEvent
}

sealed interface AnimeListEffect {
    data class NavigateToDetail(val malId: Int) : AnimeListEffect
}
