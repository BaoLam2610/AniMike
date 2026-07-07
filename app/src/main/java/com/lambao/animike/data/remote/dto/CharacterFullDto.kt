package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MVP5 Character Detail (/characters/{id}/full) — verify shape đã lưu ở
// .claude/skills/jikan-api/references/mvp5-characters-people-studio.md.
// KHÔNG khai `manga` (project chưa làm manga, xem CLAUDE.md).
@Serializable
data class CharacterFullDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val name: String? = null,
    @SerialName("name_kanji") val nameKanji: String? = null,
    val nicknames: List<String> = emptyList(),
    val favorites: Int? = null,
    val about: String? = null,
    val anime: List<CharacterAnimeRefDto> = emptyList(),
    val voices: List<CharacterVoiceRefDto> = emptyList(),
)

@Serializable
data class CharacterAnimeRefDto(
    val role: String? = null,
    val anime: AnimeDto? = null,
)

@Serializable
data class CharacterVoiceRefDto(
    val language: String? = null,
    val person: PersonRefDto? = null,
)

// Khác PersonDto (CharacterEntryDto.kt, chỉ có `name`) — endpoint này trả đủ
// mal_id/images để điều hướng sang People Detail (MVP5 mục 2, chưa code).
@Serializable
data class PersonRefDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val name: String? = null,
)
