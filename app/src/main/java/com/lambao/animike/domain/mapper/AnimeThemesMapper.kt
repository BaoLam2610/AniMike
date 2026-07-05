package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeThemesDto
import com.lambao.animike.domain.model.AnimeThemes

fun AnimeThemesDto.toDomain(): AnimeThemes = AnimeThemes(openings = openings, endings = endings)
