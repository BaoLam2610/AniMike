package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MVP5 "Top nhân vật" (/top/characters) — shape RÚT GỌN so với
// /characters/{id}/full (không có anime/manga/voices), verify shape ở
// references/mvp5-characters-people-studio.md. Chỉ khai field cần cho hiển thị
// (bỏ name_kanji/nicknames/about — không dùng ở card/lưới).
@Serializable
data class TopCharacterDto(
    @SerialName("mal_id") val malId: Int,
    val images: ImagesDto? = null,
    val name: String? = null,
    val favorites: Int? = null,
)
