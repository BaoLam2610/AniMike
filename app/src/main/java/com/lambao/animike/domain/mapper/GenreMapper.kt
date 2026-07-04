package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.GenreDto
import com.lambao.animike.domain.model.Genre

fun GenreDto.toDomain(): Genre = Genre(
    id = malId,
    name = name ?: "N/A",
)
