package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedStudioDetailEntity
import com.lambao.animike.domain.model.StudioDetail
import com.lambao.animike.domain.model.StudioExternalLink

// Delimiter ASCII hiếm gặp trong tên/URL link thật — cùng kỹ thuật
// studiosEncoded của CachedAnimeDetailMapper.
private const val ITEM_DELIMITER = "~~~"
private const val FIELD_DELIMITER = ":::"

fun CachedStudioDetailEntity.toDomain(): StudioDetail = StudioDetail(
    malId = studioId,
    name = name,
    imageUrl = imageUrl,
    establishedYear = establishedYear,
    animeCount = animeCount,
    favorites = favorites,
    about = about,
    externalLinks = decodeLinks(externalLinksEncoded),
)

fun StudioDetail.toEntity(fetchedAt: Long): CachedStudioDetailEntity = CachedStudioDetailEntity(
    studioId = malId,
    name = name,
    imageUrl = imageUrl,
    establishedYear = establishedYear,
    animeCount = animeCount,
    favorites = favorites,
    about = about,
    externalLinksEncoded = encodeLinks(externalLinks),
    fetchedAt = fetchedAt,
)

// name:::url nối bằng ITEM_DELIMITER — cùng kỹ thuật encodeStudios của
// CachedAnimeDetailMapper. Bỏ entry hỏng format khi decode.
private fun encodeLinks(links: List<StudioExternalLink>): String =
    links.joinToString(ITEM_DELIMITER) { "${it.name}$FIELD_DELIMITER${it.url}" }

private fun decodeLinks(encoded: String): List<StudioExternalLink> {
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(ITEM_DELIMITER).mapNotNull { linkStr ->
        val parts = linkStr.split(FIELD_DELIMITER, limit = 2)
        if (parts.size < 2) return@mapNotNull null
        StudioExternalLink(name = parts[0], url = parts[1])
    }
}
