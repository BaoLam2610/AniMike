package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeListEntity
import com.lambao.animike.domain.model.Anime

fun CachedAnimeListEntity.toDomain(): Anime = Anime(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    score = score,
    year = year,
)

fun Anime.toListEntity(listKey: String, position: Int, fetchedAt: Long): CachedAnimeListEntity =
    CachedAnimeListEntity(
        listKey = listKey,
        malId = malId,
        title = title,
        imageUrl = imageUrl,
        score = score,
        year = year,
        position = position,
        fetchedAt = fetchedAt,
    )
