package com.lambao.animike.ui.schedules

import androidx.compose.runtime.Immutable

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
)

sealed interface SchedulesEvent {
    data class OnDaySelected(val day: String) : SchedulesEvent
    data class OnAnimeClick(val malId: Int) : SchedulesEvent
}

sealed interface SchedulesEffect {
    data class NavigateToDetail(val malId: Int) : SchedulesEffect
}
