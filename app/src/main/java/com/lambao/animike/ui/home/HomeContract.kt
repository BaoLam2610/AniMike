package com.lambao.animike.ui.home

import com.lambao.animike.domain.model.Anime

data class SectionState(
    val isLoading: Boolean = true,
    val animeList: List<Anime> = emptyList(),
    val error: String? = null,
)

data class HomeState(
    val seasonNow: SectionState = SectionState(),
    val topAnime: SectionState = SectionState(),
    val upcoming: SectionState = SectionState(),
)

sealed interface HomeEvent {
    data class OnAnimeClick(val malId: Int) : HomeEvent
    data object OnRetrySeasonNow : HomeEvent
    data object OnRetryTopAnime : HomeEvent
    data object OnRetryUpcoming : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val malId: Int) : HomeEffect
}
