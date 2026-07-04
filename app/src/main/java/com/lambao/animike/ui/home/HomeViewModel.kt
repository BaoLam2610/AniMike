package com.lambao.animike.ui.home

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {

    // Đảm bảo không bao giờ có 2 request Jikan chạy song song — kể cả khi
    // retry một section xen vào giữa lúc chuỗi tải ban đầu (init) chưa xong
    // (jikan-api SKILL.md: "KHÔNG gọi song song nhiều endpoint").
    private val loadMutex = Mutex()

    private var seasonNowJob: Job? = null
    private var topAnimeJob: Job? = null
    private var upcomingJob: Job? = null

    init {
        // Room là nguồn hiển thị duy nhất — collect Flow suốt vòng đời màn
        // hình; refresh() chỉ quyết định KHI NÀO gọi API, không tự set list.
        observeCachedLists()
        viewModelScope.launch {
            refreshSeasonNow()
            refreshTopAnime()
            refreshUpcoming()
        }
    }

    override fun onEvent(event: HomeEvent) {
        // cancelAndJoin thay vì cancel() suông — job cũ có thể vừa thoát
        // withLock (không phải suspend point) và set state cũ đè lên state
        // của lần retry nếu không đợi nó dừng hẳn.
        when (event) {
            is HomeEvent.OnAnimeClick -> sendEffect(HomeEffect.NavigateToDetail(event.malId))

            HomeEvent.OnRetrySeasonNow -> {
                val previous = seasonNowJob
                seasonNowJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refreshSeasonNow(force = true)
                }
            }

            HomeEvent.OnRetryTopAnime -> {
                val previous = topAnimeJob
                topAnimeJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refreshTopAnime(force = true)
                }
            }

            HomeEvent.OnRetryUpcoming -> {
                val previous = upcomingJob
                upcomingJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refreshUpcoming(force = true)
                }
            }

            HomeEvent.OnPullToRefresh -> {
                val previousSeasonNow = seasonNowJob
                val previousTopAnime = topAnimeJob
                val previousUpcoming = upcomingJob
                // Gán cùng 1 job cho cả 3 biến — nếu user bấm "Thử lại" riêng 1
                // section trong lúc pull-to-refresh đang chạy, cancelAndJoin sẽ
                // nhắm đúng job đang chạy thật thay vì job cũ đã xong.
                val refreshJob = viewModelScope.launch {
                    setState { copy(isRefreshing = true) }
                    previousSeasonNow?.cancelAndJoin()
                    previousTopAnime?.cancelAndJoin()
                    previousUpcoming?.cancelAndJoin()
                    refreshSeasonNow(force = true)
                    refreshTopAnime(force = true)
                    refreshUpcoming(force = true)
                    setState { copy(isRefreshing = false) }
                }
                seasonNowJob = refreshJob
                topAnimeJob = refreshJob
                upcomingJob = refreshJob
            }
        }
    }

    private fun observeCachedLists() {
        viewModelScope.launch {
            repository.observeSeasonNow().collect { list ->
                setState { copy(seasonNow = seasonNow.copy(animeList = list)) }
            }
        }
        viewModelScope.launch {
            repository.observeTopAnime().collect { list ->
                setState { copy(topAnime = topAnime.copy(animeList = list)) }
            }
        }
        viewModelScope.launch {
            repository.observeUpcoming().collect { list ->
                setState { copy(upcoming = upcoming.copy(animeList = list)) }
            }
        }
    }

    private suspend fun refreshSeasonNow(force: Boolean = false) {
        setState { copy(seasonNow = seasonNow.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.refreshSeasonNow(force)) {
                is ApiResult.Success -> setState { copy(seasonNow = seasonNow.copy(isLoading = false)) }
                is ApiResult.Error -> setState {
                    copy(seasonNow = seasonNow.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }

    private suspend fun refreshTopAnime(force: Boolean = false) {
        setState { copy(topAnime = topAnime.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.refreshTopAnime(force)) {
                is ApiResult.Success -> setState { copy(topAnime = topAnime.copy(isLoading = false)) }
                is ApiResult.Error -> setState {
                    copy(topAnime = topAnime.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }

    private suspend fun refreshUpcoming(force: Boolean = false) {
        setState { copy(upcoming = upcoming.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.refreshUpcoming(force)) {
                is ApiResult.Success -> setState { copy(upcoming = upcoming.copy(isLoading = false)) }
                is ApiResult.Error -> setState {
                    copy(upcoming = upcoming.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }
}
