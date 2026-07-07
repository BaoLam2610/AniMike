package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedPersonDetailEntity
import com.lambao.animike.domain.model.PersonDetail

// Delimiter ASCII hiếm gặp trong tên thật — cùng kỹ thuật nicknamesEncoded
// của CachedCharacterDetailMapper.
private const val ITEM_DELIMITER = "~~~"

fun CachedPersonDetailEntity.toDomain(): PersonDetail = PersonDetail(
    malId = personId,
    name = name,
    givenName = givenName,
    familyName = familyName,
    imageUrl = imageUrl,
    alternateNames = decodeList(alternateNamesEncoded),
    birthday = birthday,
    favorites = favorites,
    about = about,
)

fun PersonDetail.toEntity(fetchedAt: Long): CachedPersonDetailEntity = CachedPersonDetailEntity(
    personId = malId,
    name = name,
    givenName = givenName,
    familyName = familyName,
    imageUrl = imageUrl,
    alternateNamesEncoded = encodeList(alternateNames),
    birthday = birthday,
    favorites = favorites,
    about = about,
    fetchedAt = fetchedAt,
)

private fun encodeList(items: List<String>): String = items.joinToString(ITEM_DELIMITER)

private fun decodeList(encoded: String): List<String> =
    if (encoded.isEmpty()) emptyList() else encoded.split(ITEM_DELIMITER)
