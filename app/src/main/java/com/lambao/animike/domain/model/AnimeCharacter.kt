package com.lambao.animike.domain.model

data class AnimeCharacter(
    val malId: Int,
    val name: String,
    val imageUrl: String?,
    val role: String,
    val voiceActorName: String?,
)
