package com.lambao.animike.ui.seasonarchive

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.SeasonArchiveRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 25

/**
 * PagingData không đặt trong UiState StateFlow (compose-expert/references/
 * paging-mvi-testing.md) — expose riêng `items` bên cạnh state/effect kế thừa
 * từ BaseViewModel, cùng pattern với SearchViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeasonArchiveViewModel @Inject constructor(
    private val repository: SeasonArchiveRepository,
) : BaseViewModel<SeasonArchiveState, SeasonArchiveEvent, SeasonArchiveEffect>(SeasonArchiveState()) {

    private val selectionFlow = MutableStateFlow<Pair<Int, String>?>(null)
    private var yearsJob: Job? = null

    val items: Flow<PagingData<Anime>> = selectionFlow
        .filterNotNull()
        .flatMapLatest { (year, season) ->
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = { repository.seasonArchivePagingSource(year, season) },
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        observeCachedYears()
        yearsJob = viewModelScope.launch { refreshYears() }
    }

    override fun onEvent(event: SeasonArchiveEvent) {
        when (event) {
            is SeasonArchiveEvent.OnYearSelected -> {
                val year = currentState().years.find { it.year == event.year } ?: return
                val season = preferredSeason(year.seasons) ?: return
                selectSeason(event.year, season)
            }

            is SeasonArchiveEvent.OnSeasonSelected -> {
                val year = currentState().selectedYear ?: return
                selectSeason(year, event.season)
            }

            is SeasonArchiveEvent.OnAnimeClick -> sendEffect(SeasonArchiveEffect.NavigateToDetail(event.malId))

            SeasonArchiveEvent.OnRetryYears -> {
                val previous = yearsJob
                yearsJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refreshYears(force = true)
                }
            }
        }
    }

    // Giữ season đang chọn nếu năm mới cũng có season đó (VD đang xem "fall",
    // đổi năm thì ưu tiên vẫn ở "fall"); chỉ fallback về season đầu tiên theo
    // seasonOrder (không phải thứ tự thô từ API) khi năm mới không có season đó.
    private fun preferredSeason(availableSeasons: List<String>): String? {
        val current = currentState().selectedSeason
        if (current != null && current in availableSeasons) return current
        return seasonOrder.firstOrNull { it in availableSeasons } ?: availableSeasons.firstOrNull()
    }

    private fun selectSeason(year: Int, season: String) {
        setState { copy(selectedYear = year, selectedSeason = season) }
        selectionFlow.value = year to season
    }

    private fun observeCachedYears() {
        viewModelScope.launch {
            repository.observeSeasonsList().collect { years ->
                setState { copy(years = years) }
                // Chọn mặc định năm/mùa đầu tiên (mới nhất) khi danh sách vừa
                // có dữ liệu và user chưa tự chọn gì.
                if (currentState().selectedYear == null) {
                    val firstYear = years.firstOrNull() ?: return@collect
                    val firstSeason = preferredSeason(firstYear.seasons) ?: return@collect
                    selectSeason(firstYear.year, firstSeason)
                }
            }
        }
    }

    private suspend fun refreshYears(force: Boolean = false) {
        setState { copy(yearsError = null) }
        when (val result = repository.refreshSeasonsList(force)) {
            is ApiResult.Success -> Unit
            is ApiResult.Error -> setState { copy(yearsError = result.error.toUserMessage()) }
        }
    }
}
