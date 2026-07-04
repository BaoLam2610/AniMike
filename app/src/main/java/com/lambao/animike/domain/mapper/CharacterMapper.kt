package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.CharacterEntryDto
import com.lambao.animike.domain.model.AnimeCharacter

fun CharacterEntryDto.toDomain(): AnimeCharacter? {
    val char = character ?: return null
    val preferredVoiceActor = voiceActors.firstOrNull { it.language == "Japanese" } ?: voiceActors.firstOrNull()
    return AnimeCharacter(
        malId = char.malId,
        name = char.name ?: "Không rõ tên",
        imageUrl = char.images?.jpg?.imageUrl,
        role = role ?: "",
        voiceActorName = preferredVoiceActor?.person?.name,
    )
}
