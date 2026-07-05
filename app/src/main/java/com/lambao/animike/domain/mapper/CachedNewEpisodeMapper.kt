package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedNewEpisodeEntity
import com.lambao.animike.domain.model.NewEpisodeRelease

fun CachedNewEpisodeEntity.toDomain(): NewEpisodeRelease = NewEpisodeRelease(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    episodeLabel = episodeLabel,
)

fun NewEpisodeRelease.toEntity(position: Int, fetchedAt: Long): CachedNewEpisodeEntity =
    CachedNewEpisodeEntity(
        malId = malId,
        title = title,
        imageUrl = imageUrl,
        episodeLabel = episodeLabel,
        position = position,
        fetchedAt = fetchedAt,
    )
