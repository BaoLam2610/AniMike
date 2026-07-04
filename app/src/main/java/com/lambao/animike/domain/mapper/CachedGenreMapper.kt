package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedGenreEntity
import com.lambao.animike.domain.model.Genre

fun CachedGenreEntity.toDomain(): Genre = Genre(id = id, name = name)

fun Genre.toEntity(fetchedAt: Long): CachedGenreEntity = CachedGenreEntity(
    id = id,
    name = name,
    fetchedAt = fetchedAt,
)
