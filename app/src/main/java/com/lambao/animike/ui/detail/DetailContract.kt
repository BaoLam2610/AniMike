package com.lambao.animike.ui.detail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.Episode

@Immutable
data class DetailState(
    val isLoading: Boolean = true,
    val detail: AnimeDetail? = null,
    val error: String? = null,
    val characters: List<AnimeCharacter> = emptyList(),
    val recommendations: List<Anime> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val reviews: List<AnimeReview> = emptyList(),
    // URL ảnh từ /pictures (poster art các thời kỳ) — chỉ URL, không cần model riêng.
    val pictures: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    // Pull-to-refresh (docs/ROADMAP.md mục 3b) — force refresh detail +
    // recommendations/reviews/pictures bất kể TTL. Episodes không cần "force"
    // vì nó vốn đã luôn gọi lại API ở mọi lần loadAll().
    val isRefreshing: Boolean = false,
)

sealed interface DetailEvent {
    data object OnRetry : DetailEvent
    data object OnPullToRefresh : DetailEvent
    // youtubeId truyền thẳng từ TrailerCard (đã có sẵn ở call site) — đỡ phải
    // đọc lại currentState().detail?.trailerYoutubeId trong ViewModel.
    data class OnTrailerClick(val youtubeId: String) : DetailEvent
    data object OnFavoriteClick : DetailEvent
    data class OnRecommendationClick(val malId: Int) : DetailEvent
    data object OnSeeAllEpisodesClick : DetailEvent
    data object OnSeeAllCharactersClick : DetailEvent
    data object OnSeeAllReviewsClick : DetailEvent
}

sealed interface DetailEffect {
    // Điều hướng RA NGOÀI app (startActivity) — phải đi qua Effect như mọi
    // side-effect khác, không gọi thẳng trong composable (quy tắc MVI ở
    // CLAUDE.md: composable chỉ render state + gửi event).
    data class OpenYoutube(val videoId: String) : DetailEffect
    data class NavigateToDetail(val malId: Int) : DetailEffect
    data class NavigateToEpisodes(val malId: Int) : DetailEffect
    data class NavigateToCharacters(val malId: Int) : DetailEffect
    data class NavigateToReviews(val malId: Int) : DetailEffect
}
