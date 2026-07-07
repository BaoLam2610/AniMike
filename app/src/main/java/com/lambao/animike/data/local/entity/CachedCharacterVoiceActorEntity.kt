package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache "Lồng tiếng bởi" ở Character Detail (MVP5, voices[] của /characters/{id}/full). */
@Entity(tableName = "cached_character_voice_actor", primaryKeys = ["characterId", "personMalId"])
data class CachedCharacterVoiceActorEntity(
    val characterId: Int,
    val personMalId: Int,
    val name: String,
    val imageUrl: String?,
    val language: String,
    val position: Int,
    val fetchedAt: Long,
)
