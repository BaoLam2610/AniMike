package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedStreamingLinkEntity
import com.lambao.animike.domain.model.StreamingLink

fun CachedStreamingLinkEntity.toDomain(): StreamingLink = StreamingLink(name = name, url = url)

fun StreamingLink.toEntity(malId: Int, position: Int, fetchedAt: Long): CachedStreamingLinkEntity =
    CachedStreamingLinkEntity(malId = malId, url = url, name = name, position = position, fetchedAt = fetchedAt)
