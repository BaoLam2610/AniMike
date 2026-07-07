package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP5 "Ê-kíp sản xuất" ở Detail (/anime/{id}/staff) — khoá theo malId của
// anime (KHÁC PersonDetail khoá theo personMalId), 1 người có thể giữ nhiều
// vai trò cùng lúc (VD ["Director", "Storyboard"]) nên positions là list.
@Immutable
data class AnimeStaffMember(
    val personMalId: Int,
    val name: String,
    val imageUrl: String?,
    val positions: List<String>,
)
