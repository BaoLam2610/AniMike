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
    // Hero giờ là slider nhiều trang (Season Now) — cần trạng thái yêu thích
    // theo TỪNG anime thay vì 1 Boolean cho hero cố định như trước.
    val favoriteIds: Set<Int> = emptySet(),
)

sealed interface HomeEvent {
    data class OnAnimeClick(val malId: Int) : HomeEvent
    data object OnRetrySeasonNow : HomeEvent
    data object OnRetryTopAnime : HomeEvent
    data object OnRetryUpcoming : HomeEvent
    data object OnPullToRefresh : HomeEvent
    data class OnHeroFavoriteClick(val malId: Int) : HomeEvent
    data object OnSeeAllTopAnimeClick : HomeEvent
    data object OnSeeAllUpcomingClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val malId: Int) : HomeEffect
    data object NavigateToTopAnime : HomeEffect
    data object NavigateToUpcoming : HomeEffect
}
