package com.lambao.animike.ui.schedules

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.ScheduledAnime

// Thứ tự cố định thứ 2 → CN dùng cho chip ngày — ngày trong tuần không đổi,
// không cần fetch từ API như SeasonArchive (SeasonArchiveContract.seasonOrder).
val weekDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

val weekDayLabels = mapOf(
    "monday" to "Thứ 2",
    "tuesday" to "Thứ 3",
    "wednesday" to "Thứ 4",
    "thursday" to "Thứ 5",
    "friday" to "Thứ 6",
    "saturday" to "Thứ 7",
    "sunday" to "Chủ nhật",
)

@Immutable
data class SchedulesState(
    val selectedDay: String = weekDays.first(),
    // Ngày dương lịch của tuần hiện tại cho từng thứ (kit Animax hiện "Sat 18",
    // "Mon 20"...) — chỉ để hiển thị, Jikan /schedules không phân biệt theo
    // ngày cụ thể (chỉ lặp lại theo thứ trong tuần).
    val dayDates: Map<String, Int> = emptyMap(),
    val favoriteMalIds: Set<Int> = emptySet(),
)

sealed interface SchedulesEvent {
    data class OnDaySelected(val day: String) : SchedulesEvent
    data class OnAnimeClick(val malId: Int) : SchedulesEvent
    data class OnFavoriteToggle(val anime: ScheduledAnime) : SchedulesEvent
}

sealed interface SchedulesEffect {
    data class NavigateToDetail(val malId: Int) : SchedulesEffect
}
