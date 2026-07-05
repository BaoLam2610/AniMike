package com.lambao.animike.ui.home

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val favoriteRepository: FavoriteRepository,
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {

    // Đảm bảo không bao giờ có 2 request Jikan chạy song song — kể cả khi
    // retry một section xen vào giữa lúc chuỗi tải ban đầu (init) chưa xong
    // (jikan-api SKILL.md: "KHÔNG gọi song song nhiều endpoint").
    private val loadMutex = Mutex()

    private var seasonNowJob: Job? = null
    private var topAnimeJob: Job? = null
    private var upcomingJob: Job? = null
    private var newEpisodesJob: Job? = null

    // Guard double-tap theo TỪNG anime — hero giờ là slider nhiều trang, dùng
    // 1 job chung sẽ drop nhầm tap hợp lệ trên trang khác khi user vuốt nhanh.
    private val heroFavoriteJobs = mutableMapOf<Int, Job>()

    private var randomAnimeJob: Job? = null

    init {
        // Room là nguồn hiển thị duy nhất — collect Flow suốt vòng đời màn
        // hình; refresh() chỉ quyết định KHI NÀO gọi API, không tự set list.
        observeCachedLists()
        observeFavoriteIds()
        viewModelScope.launch {
            refreshSeasonNow()
            refreshTopAnime()
            refreshUpcoming()
            refreshNewEpisodes()
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
                val previousNewEpisodes = newEpisodesJob
                // Gán cùng 1 job cho cả 4 biến — nếu user bấm "Thử lại" riêng 1
                // section trong lúc pull-to-refresh đang chạy, cancelAndJoin sẽ
                // nhắm đúng job đang chạy thật thay vì job cũ đã xong.
                val refreshJob = viewModelScope.launch {
                    setState { copy(isRefreshing = true) }
                    previousSeasonNow?.cancelAndJoin()
                    previousTopAnime?.cancelAndJoin()
                    previousUpcoming?.cancelAndJoin()
                    previousNewEpisodes?.cancelAndJoin()
                    refreshSeasonNow(force = true)
                    refreshTopAnime(force = true)
                    refreshUpcoming(force = true)
                    refreshNewEpisodes(force = true)
                    setState { copy(isRefreshing = false) }
                }
                seasonNowJob = refreshJob
                topAnimeJob = refreshJob
                upcomingJob = refreshJob
                newEpisodesJob = refreshJob
            }

            is HomeEvent.OnHeroFavoriteClick -> {
                // Bỏ qua nếu lần toggle trước của CHÍNH anime này chưa xong —
                // cùng lý do với DetailViewModel.OnFavoriteClick (tránh
                // double-tap ghi thừa Room).
                if (heroFavoriteJobs[event.malId]?.isActive == true) return
                val anime = currentState().seasonNow.animeList
                    .firstOrNull { it.malId == event.malId } ?: return
                heroFavoriteJobs[event.malId] = viewModelScope.launch {
                    favoriteRepository.toggleFavorite(anime)
                }
            }

            HomeEvent.OnSeeAllTopAnimeClick -> sendEffect(HomeEffect.NavigateToTopAnime)
            HomeEvent.OnSeeAllUpcomingClick -> sendEffect(HomeEffect.NavigateToUpcoming)

            HomeEvent.OnRandomAnimeClick -> {
                // Chặn double-tap trong lúc đang chờ /random/anime trả về —
                // không dùng job.isActive suông vì cần cả cờ isLoadingRandom
                // để UI đổi icon dice thành spinner.
                if (randomAnimeJob?.isActive == true) return
                randomAnimeJob = viewModelScope.launch {
                    setState { copy(isLoadingRandom = true, randomAnimeError = null) }
                    // loadMutex: cùng quy tắc "không gọi song song nhiều
                    // endpoint Jikan" với season/top/upcoming ở trên.
                    when (val result = loadMutex.withLock { repository.getRandomAnimeId() }) {
                        is ApiResult.Success -> sendEffect(HomeEffect.NavigateToDetail(result.data))
                        // Request Jikan thật (khác Room write local của
                        // OnHeroFavoriteClick) — phải báo message rõ cho user
                        // như mọi lỗi request khác trong file này, không im
                        // lặng chỉ log.
                        is ApiResult.Error -> setState { copy(randomAnimeError = result.error.toUserMessage()) }
                    }
                    setState { copy(isLoadingRandom = false) }
                }
            }

            HomeEvent.OnRetryNewEpisodes -> {
                val previous = newEpisodesJob
                newEpisodesJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refreshNewEpisodes(force = true)
                }
            }

            HomeEvent.OnSeeAllNewEpisodesClick -> sendEffect(HomeEffect.NavigateToNewEpisodes)
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
        viewModelScope.launch {
            repository.observeNewEpisodeReleases().collect { list ->
                setState { copy(newEpisodes = newEpisodes.copy(releases = list)) }
            }
        }
    }

    private fun observeFavoriteIds() {
        // Hero slider có nhiều trang, mỗi trang cần biết đã yêu thích hay chưa —
        // observe cả bảng favorite rồi rút về Set<malId> (bảng local nhỏ, rẻ hơn
        // là quản lý N flow observeIsFavorite theo trang đang hiện).
        viewModelScope.launch {
            favoriteRepository.observeFavorites().collect { favorites ->
                setState { copy(favoriteIds = favorites.mapTo(mutableSetOf()) { it.malId }) }
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

    private suspend fun refreshNewEpisodes(force: Boolean = false) {
        setState { copy(newEpisodes = newEpisodes.copy(isLoading = true, error = null)) }
        loadMutex.withLock {
            when (val result = repository.refreshNewEpisodeReleases(force)) {
                is ApiResult.Success -> setState { copy(newEpisodes = newEpisodes.copy(isLoading = false)) }
                is ApiResult.Error -> setState {
                    copy(newEpisodes = newEpisodes.copy(isLoading = false, error = result.error.toUserMessage()))
                }
            }
        }
    }
}
