package com.lambao.animike.data.remote.dto

import kotlinx.serialization.Serializable

// /anime/{id}/streaming — verify qua curl: mỗi item chỉ {name, url}, danh
// sách ngắn (~1-5 nền tảng hợp pháp: Crunchyroll/Netflix/Tubi...).
@Serializable
data class StreamingLinkDto(
    val name: String? = null,
    val url: String? = null,
)
