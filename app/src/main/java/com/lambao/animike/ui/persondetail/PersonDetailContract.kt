package com.lambao.animike.ui.persondetail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.PersonDetail
import com.lambao.animike.domain.model.PersonStaffCredit
import com.lambao.animike.domain.model.PersonVoiceRole

// Nhóm các vai diễn TRÙNG anime lại với nhau (1 người có thể lồng tiếng
// nhiều nhân vật trong cùng 1 phim) — theo yêu cầu user, xem
// PersonDetailState.groupedVoiceRoles.
@Immutable
data class VoiceRoleGroup(
    val anime: Anime,
    val roles: List<PersonVoiceRole>,
)

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

    // Nhóm theo anime.malId (LinkedHashMap giữ thứ tự xuất hiện đầu tiên của
    // mỗi anime), trong mỗi nhóm sắp Main lên trước Supporting — sortedBy ổn
    // định (stable) nên các vai cùng mức ưu tiên giữ nguyên thứ tự gốc.
    val groupedVoiceRoles: List<VoiceRoleGroup>
        get() {
            val buckets = LinkedHashMap<Int, MutableList<PersonVoiceRole>>()
            for (role in filteredVoiceRoles) {
                buckets.getOrPut(role.anime.malId) { mutableListOf() }.add(role)
            }
            return buckets.values.map { roles ->
                VoiceRoleGroup(
                    anime = roles.first().anime,
                    roles = roles.sortedBy { if (it.role.equals("Main", ignoreCase = true)) 0 else 1 },
                )
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
