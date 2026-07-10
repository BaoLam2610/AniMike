package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedTopCharacterEntity
import com.lambao.animike.domain.model.TopCharacter

fun CachedTopCharacterEntity.toDomain(): TopCharacter = TopCharacter(
    malId = malId,
    name = name,
    imageUrl = imageUrl,
    favorites = favorites,
)

fun TopCharacter.toEntity(position: Int, fetchedAt: Long): CachedTopCharacterEntity = CachedTopCharacterEntity(
    malId = malId,
    name = name,
    imageUrl = imageUrl,
    favorites = favorites,
    position = position,
    fetchedAt = fetchedAt,
)
