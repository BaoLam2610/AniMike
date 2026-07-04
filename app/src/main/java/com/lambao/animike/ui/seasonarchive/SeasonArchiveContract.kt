package com.lambao.animike.ui.seasonarchive

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.SeasonYear

// Thứ tự chuẩn winter→spring→summer→fall — dùng chung cho cả thứ tự hiển
// thị chip (SeasonArchiveScreen) và chọn season mặc định (SeasonArchiveViewModel)
// để tránh lệch thứ tự giữa 2 nơi.
val seasonOrder = listOf("winter", "spring", "summer", "fall")

@Immutable
data class SeasonArchiveState(
    val years: List<SeasonYear> = emptyList(),
    val selectedYear: Int? = null,
    val selectedSeason: String? = null,
    val yearsError: String? = null,
)

sealed interface SeasonArchiveEvent {
    data class OnYearSelected(val year: Int) : SeasonArchiveEvent
    data class OnSeasonSelected(val season: String) : SeasonArchiveEvent
    data class OnAnimeClick(val malId: Int) : SeasonArchiveEvent
    data object OnRetryYears : SeasonArchiveEvent
}

sealed interface SeasonArchiveEffect {
    data class NavigateToDetail(val malId: Int) : SeasonArchiveEffect
}
