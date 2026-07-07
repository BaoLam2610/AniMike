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
    // Bấm 1 VoiceActorItem trong "Lồng tiếng bởi" — mở People Detail (MVP5
    // mục 2), trước đây KHÔNG có onClick vì People Detail chưa tồn tại.
    data class OnVoiceActorClick(val personMalId: Int) : CharacterDetailEvent
}

sealed interface CharacterDetailEffect {
    data class NavigateToDetail(val malId: Int) : CharacterDetailEffect
    data class NavigateToPersonDetail(val personMalId: Int) : CharacterDetailEffect
}
