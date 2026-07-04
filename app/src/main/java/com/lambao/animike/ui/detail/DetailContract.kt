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
)

sealed interface DetailEvent {
    data object OnRetry : DetailEvent
    data object OnTrailerClick : DetailEvent
    data object OnFavoriteClick : DetailEvent
    data class OnRecommendationClick(val malId: Int) : DetailEvent
    data object OnSeeAllEpisodesClick : DetailEvent
    data object OnSeeAllCharactersClick : DetailEvent
    data object OnSeeAllReviewsClick : DetailEvent
}

sealed interface DetailEffect {
    data class OpenYoutube(val videoId: String) : DetailEffect
    data class NavigateToDetail(val malId: Int) : DetailEffect
    data class NavigateToEpisodes(val malId: Int) : DetailEffect
    data class NavigateToCharacters(val malId: Int) : DetailEffect
    data class NavigateToReviews(val malId: Int) : DetailEffect
}
