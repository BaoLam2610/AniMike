package com.lambao.animike.ui.favorites

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime

@Immutable
data class FavoritesState(
    val favorites: List<Anime> = emptyList(),
)

sealed interface FavoritesEvent {
    data class OnAnimeClick(val malId: Int) : FavoritesEvent
}

sealed interface FavoritesEffect {
    data class NavigateToDetail(val malId: Int) : FavoritesEffect
}
