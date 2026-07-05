package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeVideoEntity
import com.lambao.animike.domain.model.AnimeVideo

fun CachedAnimeVideoEntity.toDomain(): AnimeVideo =
    AnimeVideo(youtubeId = youtubeId, title = title, subtitle = subtitle)

fun AnimeVideo.toEntity(malId: Int, position: Int, fetchedAt: Long): CachedAnimeVideoEntity =
    CachedAnimeVideoEntity(
        malId = malId,
        youtubeId = youtubeId,
        title = title,
        subtitle = subtitle,
        position = position,
        fetchedAt = fetchedAt,
    )
