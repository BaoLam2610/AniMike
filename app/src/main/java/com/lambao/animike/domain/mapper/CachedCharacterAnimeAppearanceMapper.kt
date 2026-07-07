package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedCharacterAnimeAppearanceEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.CharacterAnimeAppearance

fun CachedCharacterAnimeAppearanceEntity.toDomain(): CharacterAnimeAppearance = CharacterAnimeAppearance(
    role = role,
    // score/year không cache riêng — nhánh "Xuất hiện trong" chỉ cần
    // title/imageUrl để render AnimeCard, không hiển thị badge điểm/năm.
    anime = Anime(malId = animeMalId, title = animeTitle, imageUrl = animeImageUrl, score = "N/A", year = null),
)

fun CharacterAnimeAppearance.toEntity(
    characterId: Int,
    position: Int,
    fetchedAt: Long,
): CachedCharacterAnimeAppearanceEntity = CachedCharacterAnimeAppearanceEntity(
    characterId = characterId,
    animeMalId = anime.malId,
    animeTitle = anime.title,
    animeImageUrl = anime.imageUrl,
    role = role,
    position = position,
    fetchedAt = fetchedAt,
)
