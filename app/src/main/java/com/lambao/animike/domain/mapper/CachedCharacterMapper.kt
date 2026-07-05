package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedCharacterEntity
import com.lambao.animike.domain.model.AnimeCharacter

fun CachedCharacterEntity.toDomain(): AnimeCharacter = AnimeCharacter(
    malId = characterId,
    name = name,
    imageUrl = imageUrl,
    role = role,
    voiceActorName = voiceActorName,
)

fun AnimeCharacter.toCharacterEntity(malId: Int, position: Int, fetchedAt: Long): CachedCharacterEntity =
    CachedCharacterEntity(
        malId = malId,
        characterId = this.malId,
        name = name,
        imageUrl = imageUrl,
        role = role,
        voiceActorName = voiceActorName,
        position = position,
        fetchedAt = fetchedAt,
    )
