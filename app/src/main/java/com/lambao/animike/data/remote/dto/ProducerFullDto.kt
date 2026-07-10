package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MVP5 Studio Detail (/producers/{id}/full) — verify shape đã lưu ở
// .claude/skills/jikan-api/references/mvp5-characters-people-studio.md.
// KHÔNG có top-level `name` (verify 2026-07-10) — tên lấy từ titles[] type
// "Default". `count` = tổng anime studio sản xuất (list anime gọi RIÊNG qua
// /anime?producers={id}, không kèm trong response này).
@Serializable
data class ProducerFullDto(
    @SerialName("mal_id") val malId: Int,
    val titles: List<ProducerTitleDto> = emptyList(),
    val images: ImagesDto? = null,
    val favorites: Int? = null,
    val established: String? = null,
    val about: String? = null,
    val count: Int? = null,
    val external: List<ProducerExternalDto> = emptyList(),
)

@Serializable
data class ProducerTitleDto(
    val type: String? = null,   // Default | Japanese | Synonym
    val title: String? = null,
)

@Serializable
data class ProducerExternalDto(
    val name: String? = null,
    val url: String? = null,
)
