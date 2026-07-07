package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedCharacterDetailEntity
import com.lambao.animike.domain.model.CharacterDetail

// Delimiter ASCII hiếm gặp trong nickname thật — cùng kỹ thuật genresEncoded
// của CachedAnimeDetailMapper.
private const val ITEM_DELIMITER = "~~~"

fun CachedCharacterDetailEntity.toDomain(): CharacterDetail = CharacterDetail(
    malId = characterId,
    name = name,
    nameKanji = nameKanji,
    imageUrl = imageUrl,
    nicknames = decodeList(nicknamesEncoded),
    favorites = favorites,
    about = about,
)

fun CharacterDetail.toEntity(fetchedAt: Long): CachedCharacterDetailEntity = CachedCharacterDetailEntity(
    characterId = malId,
    name = name,
    nameKanji = nameKanji,
    imageUrl = imageUrl,
    nicknamesEncoded = encodeList(nicknames),
    favorites = favorites,
    about = about,
    fetchedAt = fetchedAt,
)

private fun encodeList(items: List<String>): String = items.joinToString(ITEM_DELIMITER)

private fun decodeList(encoded: String): List<String> =
    if (encoded.isEmpty()) emptyList() else encoded.split(ITEM_DELIMITER)
