package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedPersonVoiceRoleEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.PersonVoiceRole

fun CachedPersonVoiceRoleEntity.toDomain(): PersonVoiceRole = PersonVoiceRole(
    role = role,
    anime = Anime(malId = animeMalId, title = animeTitle, imageUrl = animeImageUrl, score = "N/A", year = null),
    characterMalId = characterMalId,
    characterName = characterName,
    characterImageUrl = characterImageUrl,
)

fun PersonVoiceRole.toEntity(personId: Int, position: Int, fetchedAt: Long): CachedPersonVoiceRoleEntity =
    CachedPersonVoiceRoleEntity(
        personId = personId,
        animeMalId = anime.malId,
        characterMalId = characterMalId,
        animeTitle = anime.title,
        animeImageUrl = anime.imageUrl,
        characterName = characterName,
        characterImageUrl = characterImageUrl,
        role = role,
        position = position,
        fetchedAt = fetchedAt,
    )
