package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeStatisticsDto
import com.lambao.animike.domain.model.AnimeStatistics
import com.lambao.animike.domain.model.ScoreDistributionEntry

fun AnimeStatisticsDto.toDomain(): AnimeStatistics = AnimeStatistics(
    watching = watching ?: 0,
    completed = completed ?: 0,
    onHold = onHold ?: 0,
    dropped = dropped ?: 0,
    planToWatch = planToWatch ?: 0,
    total = total ?: 0,
    // mapNotNull: bỏ qua entry thiếu score thay vì crash hoặc hiện "score null".
    scoreDistribution = scores.mapNotNull { stat ->
        val score = stat.score ?: return@mapNotNull null
        ScoreDistributionEntry(score = score, votes = stat.votes ?: 0, percentage = stat.percentage ?: 0.0)
    },
)
