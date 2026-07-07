package com.lambao.animike.ui.persondetail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.PersonDetail
import com.lambao.animike.domain.model.PersonStaffCredit
import com.lambao.animike.domain.model.PersonVoiceRole

@Immutable
data class PersonDetailState(
    val isLoading: Boolean = true,
    val person: PersonDetail? = null,
    val error: String? = null,
    val staffCredits: List<PersonStaffCredit> = emptyList(),
    val voiceRoles: List<PersonVoiceRole> = emptyList(),
    // voices[] có thể vài trăm item (541 ở test case, KHÔNG phân trang) nên
    // lọc cục bộ trong bộ nhớ — cùng pattern CharactersState.filteredCharacters.
    val voiceSearchQuery: String = "",
) {
    val filteredVoiceRoles: List<PersonVoiceRole>
        get() = if (voiceSearchQuery.isBlank()) {
            voiceRoles
        } else {
            voiceRoles.filter {
                it.characterName.contains(voiceSearchQuery, ignoreCase = true) ||
                    it.anime.title.contains(voiceSearchQuery, ignoreCase = true)
            }
        }
}

sealed interface PersonDetailEvent {
    data object OnRetry : PersonDetailEvent
    data class OnAnimeClick(val malId: Int) : PersonDetailEvent
    data class OnVoiceSearchQueryChange(val query: String) : PersonDetailEvent
}

sealed interface PersonDetailEffect {
    data class NavigateToDetail(val malId: Int) : PersonDetailEffect
}
