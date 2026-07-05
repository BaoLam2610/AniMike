package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedCommunityRecommendationEntity
import com.lambao.animike.domain.model.CommunityRecommendation

fun CachedCommunityRecommendationEntity.toDomain(): CommunityRecommendation = CommunityRecommendation(
    id = id,
    firstAnimeId = firstAnimeId,
    firstAnimeTitle = firstAnimeTitle,
    firstAnimeImageUrl = firstAnimeImageUrl,
    secondAnimeId = secondAnimeId,
    secondAnimeTitle = secondAnimeTitle,
    secondAnimeImageUrl = secondAnimeImageUrl,
    content = content,
    username = username,
)

fun CommunityRecommendation.toEntity(position: Int, fetchedAt: Long): CachedCommunityRecommendationEntity =
    CachedCommunityRecommendationEntity(
        id = id,
        firstAnimeId = firstAnimeId,
        firstAnimeTitle = firstAnimeTitle,
        firstAnimeImageUrl = firstAnimeImageUrl,
        secondAnimeId = secondAnimeId,
        secondAnimeTitle = secondAnimeTitle,
        secondAnimeImageUrl = secondAnimeImageUrl,
        content = content,
        username = username,
        position = position,
        fetchedAt = fetchedAt,
    )
