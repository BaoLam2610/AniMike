package com.lambao.animike.data.remote.dto

import kotlinx.serialization.Serializable

// MVP5 "Ê-kíp sản xuất" ở Detail (/anime/{id}/staff) — verify shape đã lưu ở
// .claude/skills/jikan-api/references/mvp5-characters-people-studio.md.
// Response phẳng KHÔNG có field `pagination`. Tái dùng PersonRefDto
// (mal_id/images/name, khai ở CharacterFullDto.kt) — cùng shape.
@Serializable
data class StaffEntryDto(
    val person: PersonRefDto? = null,
    val positions: List<String> = emptyList(),
)
