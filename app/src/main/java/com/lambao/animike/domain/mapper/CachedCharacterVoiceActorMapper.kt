package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedCharacterVoiceActorEntity
import com.lambao.animike.domain.model.CharacterVoiceActor

fun CachedCharacterVoiceActorEntity.toDomain(): CharacterVoiceActor = CharacterVoiceActor(
    personMalId = personMalId,
    name = name,
    imageUrl = imageUrl,
    language = language,
)

fun CharacterVoiceActor.toEntity(
    characterId: Int,
    position: Int,
    fetchedAt: Long,
): CachedCharacterVoiceActorEntity = CachedCharacterVoiceActorEntity(
    characterId = characterId,
    personMalId = personMalId,
    name = name,
    imageUrl = imageUrl,
    language = language,
    position = position,
    fetchedAt = fetchedAt,
)
