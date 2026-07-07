package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP5 People/Seiyuu Detail — CHỈ field scalar của người, giống CharacterDetail
// không ôm theo staffCredits/voiceRoles. 2 danh sách là 2 Flow riêng ở
// PersonDetailRepository dù cùng đến từ 1 API call (/people/{id}/full).
@Immutable
data class PersonDetail(
    val malId: Int,
    val name: String,
    val givenName: String?,
    val familyName: String?,
    val imageUrl: String?,
    val alternateNames: List<String>,
    val birthday: String?,
    val favorites: Int,
    val about: String?,
)

// Credit STAFF (đạo diễn, ADR...) của người này — KHÁC voiceRoles bên dưới.
// positions là LIST (không phải String đơn) — 1 người có thể giữ nhiều vai
// trò trên CÙNG 1 anime (VD đạo diễn kiêm storyboard), Jikan trả 2 entry
// riêng cùng anime.mal_id trong trường hợp đó (xem PersonDetailMapper.toStaffCredits
// — gộp theo anime.malId, KHÔNG distinctBy đơn giản như bản đầu vì sẽ mất
// credit thật, phát hiện qua review). Cùng pattern AnimeStaffMember.positions.
@Immutable
data class PersonStaffCredit(
    val positions: List<String>,
    val anime: Anime,
)

// Credit LỒNG TIẾNG — có thể vài trăm item/người (541 ở test case verify,
// KHÔNG phân trang) nên UI phải local-search (xem PersonDetailContract).
@Immutable
data class PersonVoiceRole(
    val role: String,
    val anime: Anime,
    val characterMalId: Int,
    val characterName: String,
    val characterImageUrl: String?,
)
