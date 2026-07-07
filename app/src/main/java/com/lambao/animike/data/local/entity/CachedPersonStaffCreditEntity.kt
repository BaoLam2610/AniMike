package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache "Vai trò sản xuất" ở People Detail (MVP5, anime[] của /people/{id}/full). */
@Entity(tableName = "cached_person_staff_credit", primaryKeys = ["personId", "animeMalId"])
data class CachedPersonStaffCreditEntity(
    val personId: Int,
    val animeMalId: Int,
    val animeTitle: String,
    val animeImageUrl: String?,
    // Encode bằng delimiter ASCII (xem CachedPersonStaffCreditMapper.kt) — 1
    // người có thể giữ NHIỀU vai trò trên CÙNG 1 anime, cùng kỹ thuật
    // positionsEncoded của CachedAnimeStaffMemberEntity. Tên field khác
    // "position" (cột sắp xếp chuẩn của mọi bảng list trong app) để tránh
    // nhầm lẫn.
    val positionsEncoded: String,
    val position: Int,
    val fetchedAt: Long,
)
