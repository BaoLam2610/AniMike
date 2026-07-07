package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP5 Character Detail — CHỈ field scalar của nhân vật, giống AnimeDetail
// không ôm theo characters/recommendations. animeAppearances/voiceActors là
// 2 danh sách riêng (xem CharacterAnimeAppearance/CharacterVoiceActor), quan
// sát qua 2 Flow riêng ở CharacterDetailRepository dù cả 3 cùng đến từ 1 API
// call (/characters/{id}/full) — vì shape khác hẳn (list có cấu trúc, không
// phải string) nên không delimiter-encode chung vào 1 row như genres.
@Immutable
data class CharacterDetail(
    val malId: Int,
    val name: String,
    val nameKanji: String?,
    val imageUrl: String?,
    val nicknames: List<String>,
    val favorites: Int,
    val about: String?,
)

@Immutable
data class CharacterAnimeAppearance(
    val role: String,
    val anime: Anime,
)

// personMalId để sau này điều hướng sang People Detail (MVP5 mục 2, chưa
// code) — xem docs/ROADMAP.md.
@Immutable
data class CharacterVoiceActor(
    val personMalId: Int,
    val name: String,
    val imageUrl: String?,
    val language: String,
)
