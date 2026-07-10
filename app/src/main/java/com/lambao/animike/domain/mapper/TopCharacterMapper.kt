package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.TopCharacterDto
import com.lambao.animike.domain.model.TopCharacter

fun TopCharacterDto.toDomain(): TopCharacter = TopCharacter(
    malId = malId,
    name = name ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    favorites = favorites ?: 0,
)
