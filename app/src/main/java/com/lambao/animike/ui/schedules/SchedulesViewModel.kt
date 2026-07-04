package com.lambao.animike.ui.schedules

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.SchedulesRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

private const val PAGE_SIZE = 25

// Calendar.DAY_OF_WEEK: SUNDAY=1..SATURDAY=7 — map sang giá trị filter của
// Jikan /schedules (monday..sunday) để mặc định chọn đúng thứ hôm nay.
private fun todayAsWeekDay(): String {
    val calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val index = (calendarDay + 5) % 7 // Calendar.SUNDAY(1) -> index 6 ("sunday")
    return weekDays[index]
}

/**
 * PagingData không đặt trong UiState StateFlow (compose-expert/references/
 * paging-mvi-testing.md) — expose riêng `items` bên cạnh state/effect, cùng
 * pattern với SearchViewModel/SeasonArchiveViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val repository: SchedulesRepository,
) : BaseViewModel<SchedulesState, SchedulesEvent, SchedulesEffect>(
    SchedulesState(selectedDay = todayAsWeekDay()),
) {

    // Đọc lại từ state (đã set bằng todayAsWeekDay() ở super() phía trên) thay vì
    // gọi todayAsWeekDay() lần 2 — tránh 2 giá trị lệch nhau nếu gọi đúng lúc giao ngày.
    private val dayFlow = MutableStateFlow(currentState().selectedDay)

    val items: Flow<PagingData<Anime>> = dayFlow
        .flatMapLatest { day ->
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = { repository.schedulePagingSource(day) },
            ).flow
        }
        .cachedIn(viewModelScope)

    override fun onEvent(event: SchedulesEvent) {
        when (event) {
            is SchedulesEvent.OnDaySelected -> {
                setState { copy(selectedDay = event.day) }
                dayFlow.value = event.day
            }

            is SchedulesEvent.OnAnimeClick -> sendEffect(SchedulesEffect.NavigateToDetail(event.malId))
        }
    }
}
