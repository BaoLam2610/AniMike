package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.EpisodeDto
import com.lambao.animike.domain.model.Episode

fun EpisodeDto.toDomain(): Episode = Episode(
    number = malId,
    title = title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
)
