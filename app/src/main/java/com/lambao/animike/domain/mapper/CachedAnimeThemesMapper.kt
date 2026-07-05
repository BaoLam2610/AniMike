package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeThemesEntity
import com.lambao.animike.domain.model.AnimeThemes

// Delimiter ASCII hiếm gặp trong tên bài hát/nghệ sĩ thật — cùng kỹ thuật
// với CachedAnimeDetailMapper.
private const val ITEM_DELIMITER = "~~~"

fun CachedAnimeThemesEntity.toDomain(): AnimeThemes = AnimeThemes(
    openings = decodeList(openingsEncoded),
    endings = decodeList(endingsEncoded),
)

// malId truyền riêng (khác AnimeDetail.toEntity) — AnimeThemes không tự
// mang mal_id, giống lý do ở CachedAnimeStatisticsMapper.
fun AnimeThemes.toEntity(malId: Int, fetchedAt: Long): CachedAnimeThemesEntity = CachedAnimeThemesEntity(
    malId = malId,
    openingsEncoded = encodeList(openings),
    endingsEncoded = encodeList(endings),
    fetchedAt = fetchedAt,
)

private fun encodeList(items: List<String>): String = items.joinToString(ITEM_DELIMITER)

private fun decodeList(encoded: String): List<String> =
    if (encoded.isEmpty()) emptyList() else encoded.split(ITEM_DELIMITER)
