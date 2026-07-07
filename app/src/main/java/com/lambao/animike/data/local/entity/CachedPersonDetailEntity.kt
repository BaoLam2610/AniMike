package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache People/Seiyuu Detail (MVP5, /people/{id}/full) — 1 row/người, giống cached_character_detail. */
@Entity(tableName = "cached_person_detail")
data class CachedPersonDetailEntity(
    @PrimaryKey val personId: Int,
    val name: String,
    val givenName: String?,
    val familyName: String?,
    val imageUrl: String?,
    // Encode bằng delimiter ASCII (xem CachedPersonDetailMapper.kt) — cùng kỹ
    // thuật nicknamesEncoded của CachedCharacterDetailEntity.
    val alternateNamesEncoded: String,
    val birthday: String?,
    val favorites: Int,
    val about: String?,
    val fetchedAt: Long,
)
