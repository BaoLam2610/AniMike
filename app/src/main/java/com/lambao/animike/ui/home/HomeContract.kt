package com.lambao.animike.ui.home

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.NewEpisodeRelease

@Immutable
data class SectionState(
    val isLoading: Boolean = true,
    val animeList: List<Anime> = emptyList(),
    val error: String? = null,
)

// Không tái dùng SectionState<Anime> (đổi kiểu generic sẽ động tới toàn bộ
// SeasonNow/TopAnime/Upcoming đang dùng non-generic) — NewEpisodeRelease
// khác Anime shape (không score/year, có episodeLabel) nên tách state riêng.
@Immutable
data class NewEpisodeSectionState(
    val isLoading: Boolean = true,
    val releases: List<NewEpisodeRelease> = emptyList(),
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
    // MVP4 "Hôm nay xem gì?" — true trong lúc chờ /random/anime trả về, dùng
    // để chặn double-tap + đổi icon dice thành spinner trên card.
    val isLoadingRandom: Boolean = false,
    // Message lỗi khi /random/anime fail — hiện inline dưới RandomAnimeCard
    // (giống SectionError), không im lặng bỏ qua như trước (jikan-api SKILL.md
    // yêu cầu mọi lỗi request thật đều phải có message rõ cho user).
    val randomAnimeError: String? = null,
    // MVP4 "Tập mới phát hành" (kit Animax "New Episode Releases").
    val newEpisodes: NewEpisodeSectionState = NewEpisodeSectionState(),
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
    data object OnRandomAnimeClick : HomeEvent
    data object OnRetryNewEpisodes : HomeEvent
    data object OnSeeAllNewEpisodesClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val malId: Int) : HomeEffect
    data object NavigateToTopAnime : HomeEffect
    data object NavigateToUpcoming : HomeEffect
    data object NavigateToNewEpisodes : HomeEffect
}
