package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedSeasonYearEntity
import com.lambao.animike.data.remote.dto.SeasonYearDto
import com.lambao.animike.domain.model.SeasonYear

fun SeasonYearDto.toDomain(): SeasonYear = SeasonYear(year = year, seasons = seasons)

fun CachedSeasonYearEntity.toDomain(): SeasonYear = SeasonYear(
    year = year,
    seasons = if (seasonsCsv.isEmpty()) emptyList() else seasonsCsv.split(","),
)

fun SeasonYear.toEntity(fetchedAt: Long): CachedSeasonYearEntity = CachedSeasonYearEntity(
    year = year,
    seasonsCsv = seasons.joinToString(","),
    fetchedAt = fetchedAt,
)
