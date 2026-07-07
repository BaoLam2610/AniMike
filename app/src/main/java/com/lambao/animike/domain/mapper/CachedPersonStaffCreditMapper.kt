package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedPersonStaffCreditEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.PersonStaffCredit

// Delimiter ASCII hiếm gặp trong tên chức vụ thật — cùng kỹ thuật
// positionsEncoded của CachedAnimeStaffMapper.
private const val ITEM_DELIMITER = "~~~"

fun CachedPersonStaffCreditEntity.toDomain(): PersonStaffCredit = PersonStaffCredit(
    positions = if (positionsEncoded.isEmpty()) emptyList() else positionsEncoded.split(ITEM_DELIMITER),
    anime = Anime(malId = animeMalId, title = animeTitle, imageUrl = animeImageUrl, score = "N/A", year = null),
)

fun PersonStaffCredit.toEntity(personId: Int, position: Int, fetchedAt: Long): CachedPersonStaffCreditEntity =
    CachedPersonStaffCreditEntity(
        personId = personId,
        animeMalId = anime.malId,
        animeTitle = anime.title,
        animeImageUrl = anime.imageUrl,
        positionsEncoded = positions.joinToString(ITEM_DELIMITER),
        position = position,
        fetchedAt = fetchedAt,
    )
