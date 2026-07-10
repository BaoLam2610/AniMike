package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP5 "Top nhân vật" — item của bảng xếp hạng (/top/characters). Bấm mở
// Character Detail (đã có từ MVP5 mục 1). favorites dùng cho badge "❤ 180K".
@Immutable
data class TopCharacter(
    val malId: Int,
    val name: String,
    val imageUrl: String?,
    val favorites: Int,
)
