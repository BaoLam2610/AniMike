package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedPictureEntity

fun CachedPictureEntity.toDomain(): String = url

fun String.toPictureEntity(malId: Int, position: Int, fetchedAt: Long): CachedPictureEntity =
    CachedPictureEntity(malId = malId, url = this, position = position, fetchedAt = fetchedAt)
