package com.lambao.animike.ui.home

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        viewModelScope.launch {
            loadSeasonNow()
            loadTopAnime()
            loadUpcoming()
        }
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnAnimeClick -> sendEffect(HomeEffect.NavigateToDetail(event.malId))
            HomeEvent.OnRetrySeasonNow -> {
                seasonNowJob?.cancel()
                seasonNowJob = viewModelScope.launch { loadSeasonNow() }
            }

            HomeEvent.OnRetryTopAnime -> {
                topAnimeJob?.cancel()
                topAnimeJob = viewModelScope.launch { loadTopAnime() }
            }

            HomeEvent.OnRetryUpcoming -> {
                upcomingJob?.cancel()
                upcomingJob = viewModelScope.launch { loadUpcoming() }
            }
        }
    }

    private suspend fun loadSeasonNow() {
        setState { copy(seasonNow = seasonNow.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.getSeasonNow()) {
                is ApiResult.Success -> setState {
                    copy(seasonNow = SectionState(isLoading = false, animeList = result.data))
                }

                is ApiResult.Error -> setState {
                    copy(seasonNow = seasonNow.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }

    private suspend fun loadTopAnime() {
        setState { copy(topAnime = topAnime.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.getTopAnime()) {
                is ApiResult.Success -> setState {
                    copy(topAnime = SectionState(isLoading = false, animeList = result.data))
                }

                is ApiResult.Error -> setState {
                    copy(topAnime = topAnime.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }

    private suspend fun loadUpcoming() {
        setState { copy(upcoming = upcoming.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.getUpcoming()) {
                is ApiResult.Success -> setState {
                    copy(upcoming = SectionState(isLoading = false, animeList = result.data))
                }

                is ApiResult.Error -> setState {
                    copy(upcoming = upcoming.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }
}
