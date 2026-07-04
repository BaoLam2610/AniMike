package com.lambao.animike.ui.schedules

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.data.repository.SchedulesRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.ScheduledAnime
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 25

// Calendar.DAY_OF_WEEK: SUNDAY=1..SATURDAY=7 — map sang giá trị filter của
// Jikan /schedules (monday..sunday) để mặc định chọn đúng thứ hôm nay.
private fun todayAsWeekDay(): String {
    val calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val index = (calendarDay + 5) % 7 // Calendar.SUNDAY(1) -> index 6 ("sunday")
    return weekDays[index]
}

// Ngày dương lịch thứ 2->CN của tuần hiện tại — chỉ để hiển thị trên chip
// (SchedulesState.dayDates), không ảnh hưởng query (Jikan lặp lại theo thứ).
private fun currentWeekDates(): Map<String, Int> {
    val calendar = Calendar.getInstance()
    val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = (calendarDay + 5) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    // Vòng lặp tường minh thay vì mutate `calendar` bên trong lambda của
    // associateWith — thứ tự tính ngày phụ thuộc chặt vào việc duyệt tuần tự
    // đúng thứ tự weekDays (Mon->Sun), viết rõ ràng để tránh nhầm khi sửa sau.
    val dates = LinkedHashMap<String, Int>()
    for (day in weekDays) {
        dates[day] = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dates
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
    private val favoriteRepository: FavoriteRepository,
) : BaseViewModel<SchedulesState, SchedulesEvent, SchedulesEffect>(
    SchedulesState(selectedDay = todayAsWeekDay(), dayDates = currentWeekDates()),
) {

    // Đọc lại từ state (đã set bằng todayAsWeekDay() ở super() phía trên) thay vì
    // gọi todayAsWeekDay() lần 2 — tránh 2 giá trị lệch nhau nếu gọi đúng lúc giao ngày.
    private val dayFlow = MutableStateFlow(currentState().selectedDay)

    val items: Flow<PagingData<ScheduledAnime>> = dayFlow
        .flatMapLatest { day ->
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = { repository.schedulePagingSource(day) },
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        // Chỉ cần tập malId để tô trạng thái nút "+ Yêu thích" trên từng dòng —
        // 1 subscription duy nhất bất kể list dài bao nhiêu, không phải mỗi
        // dòng tự observeIsFavorite riêng (tránh N subscription cho N item).
        viewModelScope.launch {
            favoriteRepository.observeFavorites()
                .map { list -> list.map { it.malId }.toSet() }
                .collect { ids -> setState { copy(favoriteMalIds = ids) } }
        }
    }

    override fun onEvent(event: SchedulesEvent) {
        when (event) {
            is SchedulesEvent.OnDaySelected -> {
                setState { copy(selectedDay = event.day) }
                dayFlow.value = event.day
            }

            is SchedulesEvent.OnAnimeClick -> sendEffect(SchedulesEffect.NavigateToDetail(event.malId))

            is SchedulesEvent.OnFavoriteToggle -> {
                // Không guard bằng 1 Job chung như Home/Detail — list có nhiều
                // item độc lập, guard chung sẽ chặn nhầm việc toggle item khác
                // trong lúc item trước đang ghi. Race trên CÙNG 1 item (double-tap
                // rất nhanh) vẫn an toàn nhờ @Transaction trong FavoriteDao.toggle().
                val anime = event.anime
                viewModelScope.launch {
                    favoriteRepository.toggleFavorite(
                        Anime(
                            malId = anime.malId,
                            title = anime.title,
                            imageUrl = anime.imageUrl,
                            score = anime.score,
                            year = anime.year,
                        ),
                    )
                }
            }
        }
    }
}
