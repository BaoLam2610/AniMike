package com.lambao.animike.ui.home

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime

@Immutable
data class SectionState(
    val isLoading: Boolean = true,
    val animeList: List<Anime> = emptyList(),
    val error: String? = null,
)

@Immutable
data class HomeState(
    val seasonNow: SectionState = SectionState(),
    val topAnime: SectionState = SectionState(),
    val upcoming: SectionState = SectionState(),
    val isRefreshing: Boolean = false,
    // Hero header (kit Animax) = anime đầu tiên của Season Now — hiển thị
    // trạng thái yêu thích riêng vì đây là nút hành động, không chỉ card.
    val heroIsFavorite: Boolean = false,
)

sealed interface HomeEvent {
    data class OnAnimeClick(val malId: Int) : HomeEvent
    data object OnRetrySeasonNow : HomeEvent
    data object OnRetryTopAnime : HomeEvent
    data object OnRetryUpcoming : HomeEvent
    data object OnPullToRefresh : HomeEvent
    data object OnHeroFavoriteClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val malId: Int) : HomeEffect
}
