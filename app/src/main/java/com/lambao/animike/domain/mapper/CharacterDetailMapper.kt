package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.CharacterAnimeRefDto
import com.lambao.animike.data.remote.dto.CharacterFullDto
import com.lambao.animike.data.remote.dto.CharacterVoiceRefDto
import com.lambao.animike.domain.model.CharacterAnimeAppearance
import com.lambao.animike.domain.model.CharacterDetail
import com.lambao.animike.domain.model.CharacterVoiceActor

fun CharacterFullDto.toDomain(): CharacterDetail = CharacterDetail(
    malId = malId,
    name = name ?: "Không rõ tên",
    nameKanji = nameKanji,
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    // distinct(): Jikan không đảm bảo nicknames không trùng phần tử — dùng
    // trực tiếp làm LazyRow key (NicknameChips) nên trùng sẽ crash "Key ...
    // was already used" (phát hiện qua compose-reviewer, sửa ngay).
    nicknames = nicknames.distinct(),
    favorites = favorites ?: 0,
    about = about,
)

fun CharacterAnimeRefDto.toDomain(): CharacterAnimeAppearance? {
    val animeRef = anime ?: return null
    return CharacterAnimeAppearance(role = role ?: "", anime = animeRef.toDomain())
}

fun CharacterVoiceRefDto.toDomain(): CharacterVoiceActor? {
    val personRef = person ?: return null
    return CharacterVoiceActor(
        personMalId = personRef.malId,
        name = personRef.name ?: "Không rõ tên",
        imageUrl = personRef.images?.jpg?.largeImageUrl ?: personRef.images?.jpg?.imageUrl,
        language = language ?: "",
    )
}
