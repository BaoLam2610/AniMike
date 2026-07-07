package com.lambao.animike.data.local.entity

import androidx.room.Entity

/**
 * Cache "Vai diễn lồng tiếng" ở People Detail (MVP5, voices[] của
 * /people/{id}/full) — PK có cả animeMalId lẫn characterMalId vì 1 người có
 * thể lồng tiếng NHIỀU nhân vật khác nhau trong CÙNG 1 anime (hiếm nhưng
 * không loại trừ).
 */
@Entity(tableName = "cached_person_voice_role", primaryKeys = ["personId", "animeMalId", "characterMalId"])
data class CachedPersonVoiceRoleEntity(
    val personId: Int,
    val animeMalId: Int,
    val characterMalId: Int,
    val animeTitle: String,
    val animeImageUrl: String?,
    val characterName: String,
    val characterImageUrl: String?,
    val role: String,
    val position: Int,
    val fetchedAt: Long,
)
