package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.FavoriteEntity
import com.lambao.animike.domain.model.Anime

fun FavoriteEntity.toDomain(): Anime = Anime(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    score = score,
    year = year,
)

fun Anime.toFavoriteEntity(addedAt: Long): FavoriteEntity = FavoriteEntity(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    score = score,
    year = year,
    addedAt = addedAt,
)
