package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeStaffMemberEntity
import com.lambao.animike.domain.model.AnimeStaffMember

private const val ITEM_DELIMITER = "~~~"

fun CachedAnimeStaffMemberEntity.toDomain(): AnimeStaffMember = AnimeStaffMember(
    personMalId = personMalId,
    name = name,
    imageUrl = imageUrl,
    positions = if (positionsEncoded.isEmpty()) emptyList() else positionsEncoded.split(ITEM_DELIMITER),
)

fun AnimeStaffMember.toEntity(malId: Int, position: Int, fetchedAt: Long): CachedAnimeStaffMemberEntity =
    CachedAnimeStaffMemberEntity(
        malId = malId,
        personMalId = personMalId,
        name = name,
        imageUrl = imageUrl,
        positionsEncoded = positions.joinToString(ITEM_DELIMITER),
        position = position,
        fetchedAt = fetchedAt,
    )
