package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MVP5 People/Seiyuu Detail (/people/{id}/full) — verify shape đã lưu ở
// .claude/skills/jikan-api/references/mvp5-characters-people-studio.md.
// KHÔNG khai `manga` (project chưa làm manga, xem CLAUDE.md) — 0 item ở test
// case verify nên bỏ qua an toàn.
@Serializable
data class PersonFullDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val name: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("alternate_names") val alternateNames: List<String> = emptyList(),
    val birthday: String? = null,
    val favorites: Int? = null,
    val about: String? = null,
    val anime: List<PersonStaffRefDto> = emptyList(),
    val voices: List<PersonVoiceRefDto> = emptyList(),
)

// Credit STAFF (đạo diễn, ADR...) — KHÁC voices (lồng tiếng) bên dưới.
@Serializable
data class PersonStaffRefDto(
    val position: String? = null,
    val anime: AnimeDto? = null,
)

@Serializable
data class PersonVoiceRefDto(
    val role: String? = null,
    val anime: AnimeDto? = null,
    val character: PersonVoiceCharacterRefDto? = null,
)

@Serializable
data class PersonVoiceCharacterRefDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val name: String? = null,
)
