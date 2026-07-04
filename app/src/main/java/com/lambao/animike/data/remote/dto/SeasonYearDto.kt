package com.lambao.animike.data.remote.dto

import kotlinx.serialization.Serializable

// year là field định danh của resource này (tương tự vai trò của mal_id ở
// anime resource) — không nullable, khớp yêu cầu @PrimaryKey ở Room entity.
@Serializable
data class SeasonYearDto(
    val year: Int,
    val seasons: List<String> = emptyList(),
)
