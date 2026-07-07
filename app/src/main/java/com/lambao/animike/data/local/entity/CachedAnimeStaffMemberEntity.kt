package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache "Ê-kíp sản xuất" ở Detail (MVP5, /anime/{id}/staff) — khoá theo malId của anime. */
@Entity(tableName = "cached_anime_staff_member", primaryKeys = ["malId", "personMalId"])
data class CachedAnimeStaffMemberEntity(
    val malId: Int,
    val personMalId: Int,
    val name: String,
    val imageUrl: String?,
    // Encode bằng delimiter ASCII — cùng kỹ thuật nicknamesEncoded.
    val positionsEncoded: String,
    val position: Int,
    val fetchedAt: Long,
)
