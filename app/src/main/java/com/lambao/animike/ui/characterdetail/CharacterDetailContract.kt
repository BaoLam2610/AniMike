package com.lambao.animike.ui.characterdetail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.CharacterAnimeAppearance
import com.lambao.animike.domain.model.CharacterDetail
import com.lambao.animike.domain.model.CharacterVoiceActor

@Immutable
data class CharacterDetailState(
    val isLoading: Boolean = true,
    val character: CharacterDetail? = null,
    val error: String? = null,
    val animeAppearances: List<CharacterAnimeAppearance> = emptyList(),
    val voiceActors: List<CharacterVoiceActor> = emptyList(),
)

sealed interface CharacterDetailEvent {
    data object OnRetry : CharacterDetailEvent
    data class OnAnimeClick(val malId: Int) : CharacterDetailEvent
}

sealed interface CharacterDetailEffect {
    data class NavigateToDetail(val malId: Int) : CharacterDetailEffect
}
