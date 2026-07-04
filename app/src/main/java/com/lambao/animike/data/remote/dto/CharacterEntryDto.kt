package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterEntryDto(
    val character: CharacterDto? = null,
    val role: String? = null,
    @SerialName("voice_actors") val voiceActors: List<VoiceActorDto> = emptyList(),
)

@Serializable
data class CharacterDto(
    @SerialName("mal_id") val malId: Int,
    val name: String? = null,
    val images: ImagesDto? = null,
)

@Serializable
data class VoiceActorDto(
    val person: PersonDto? = null,
    val language: String? = null,
)

@Serializable
data class PersonDto(
    val name: String? = null,
)
