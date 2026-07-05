package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeStatisticsEntity
import com.lambao.animike.domain.model.AnimeStatistics
import com.lambao.animike.domain.model.ScoreDistributionEntry

// Delimiter ASCII hiếm gặp — cùng kỹ thuật với CachedAnimeDetailMapper (mỗi
// mapper tự khai riêng, không cần TypeConverter/JSON cho vài số liệu này).
private const val ITEM_DELIMITER = "~~~"
private const val FIELD_DELIMITER = ":::"

fun CachedAnimeStatisticsEntity.toDomain(): AnimeStatistics = AnimeStatistics(
    watching = watching,
    completed = completed,
    onHold = onHold,
    dropped = dropped,
    planToWatch = planToWatch,
    total = total,
    scoreDistribution = decodeScores(scoreDistributionEncoded),
)

// malId truyền riêng (khác AnimeDetail.toEntity) vì response /statistics
// không tự mang mal_id — domain model AnimeStatistics vốn không có field này.
fun AnimeStatistics.toEntity(malId: Int, fetchedAt: Long): CachedAnimeStatisticsEntity = CachedAnimeStatisticsEntity(
    malId = malId,
    watching = watching,
    completed = completed,
    onHold = onHold,
    dropped = dropped,
    planToWatch = planToWatch,
    total = total,
    scoreDistributionEncoded = encodeScores(scoreDistribution),
    fetchedAt = fetchedAt,
)

private fun encodeScores(entries: List<ScoreDistributionEntry>): String =
    entries.joinToString(ITEM_DELIMITER) { "${it.score}$FIELD_DELIMITER${it.votes}$FIELD_DELIMITER${it.percentage}" }

private fun decodeScores(encoded: String): List<ScoreDistributionEntry> {
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(ITEM_DELIMITER).mapNotNull { entryStr ->
        val parts = entryStr.split(FIELD_DELIMITER)
        if (parts.size != 3) return@mapNotNull null
        val score = parts[0].toIntOrNull() ?: return@mapNotNull null
        val votes = parts[1].toIntOrNull() ?: return@mapNotNull null
        val percentage = parts[2].toDoubleOrNull() ?: return@mapNotNull null
        ScoreDistributionEntry(score = score, votes = votes, percentage = percentage)
    }
}
