package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedPictureEntity
import com.lambao.animike.domain.model.Picture

fun CachedPictureEntity.toDomain(): Picture = Picture(thumbnailUrl = thumbnailUrl, fullUrl = fullUrl)

fun Picture.toEntity(malId: Int, position: Int, fetchedAt: Long): CachedPictureEntity =
    CachedPictureEntity(malId = malId, thumbnailUrl = thumbnailUrl, fullUrl = fullUrl, position = position, fetchedAt = fetchedAt)
