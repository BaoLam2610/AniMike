package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

/** "Biểu đồ phân bố điểm + số người xem" (MVP4, /anime/{id}/statistics). */
@Immutable
data class AnimeStatistics(
    val watching: Int,
    val completed: Int,
    val onHold: Int,
    val dropped: Int,
    val planToWatch: Int,
    val total: Int,
    // LUÔN đúng 10 phần tử (score 1-10) theo verify qua curl, nhưng không ép
    // kiểu cứng — UI tự sort/hiển thị theo thứ tự cần.
    val scoreDistribution: List<ScoreDistributionEntry>,
)

@Immutable
data class ScoreDistributionEntry(
    val score: Int,
    val votes: Int,
    val percentage: Double,
)
