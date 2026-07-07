package com.lambao.animike.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AnimeDetailRepository,
    private val favoriteRepository: FavoriteRepository,
) : BaseViewModel<DetailState, DetailEvent, DetailEffect>(DetailState()) {

    private val malId: Int = checkNotNull(savedStateHandle[Routes.DETAIL_ARG_MAL_ID])

    // Cùng lý do với HomeViewModel: đảm bảo các lệnh gọi Jikan trong loadAll
    // không bao giờ chạy song song, kể cả khi retry xen vào.
    private val loadMutex = Mutex()
    private var loadJob: Job? = null
    private var favoriteJob: Job? = null

    init {
        // Room là nguồn hiển thị duy nhất cho detail — collect Flow suốt vòng
        // đời màn hình; loadAll() chỉ quyết định KHI NÀO gọi API. Riêng
        // Episodes KHÔNG có Flow riêng — one-shot, set thẳng trong loadAll().
        observeCachedDetail()
        observeCharacters()
        observeStaff()
        observeRecommendations()
        observeReviewPreview()
        observePictures()
        observeThemes()
        observeStreamingLinks()
        observeVideos()
        observeFavoriteStatus()
        loadJob = viewModelScope.launch { loadAll() }
    }

    override fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.OnRetry -> {
                // cancelAndJoin thay vì cancel() suông — job cũ có thể vừa
                // thoát withLock (không phải suspend point) và set state cũ
                // đè lên state của lần retry nếu không đợi nó dừng hẳn.
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    loadAll(force = true)
                }
            }

            DetailEvent.OnPullToRefresh -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    setState { copy(isRefreshing = true) }
                    previous?.cancelAndJoin()
                    loadAll(force = true)
                    setState { copy(isRefreshing = false) }
                }
            }

            is DetailEvent.OnTrailerClick -> sendEffect(DetailEffect.OpenYoutube(event.youtubeId))

            is DetailEvent.OnStreamingLinkClick -> sendEffect(DetailEffect.OpenExternalUrl(event.url))

            DetailEvent.OnFavoriteClick -> {
                // Bỏ qua nếu lần toggle trước chưa xong — tránh double-tap bắn
                // nhiều coroutine gần như đồng thời (dù DAO đã transaction-safe,
                // vẫn nên chặn sớm ở đây để không lãng phí ghi Room thừa).
                if (favoriteJob?.isActive == true) return
                val detail = currentState().detail ?: return
                favoriteJob = viewModelScope.launch {
                    favoriteRepository.toggleFavorite(
                        Anime(
                            malId = detail.malId,
                            title = detail.title,
                            imageUrl = detail.imageUrl,
                            score = detail.score,
                            year = detail.year,
                        ),
                    )
                }
            }

            is DetailEvent.OnRecommendationClick -> sendEffect(DetailEffect.NavigateToDetail(event.malId))

            DetailEvent.OnSeeAllEpisodesClick -> sendEffect(DetailEffect.NavigateToEpisodes(malId))

            DetailEvent.OnSeeAllCharactersClick -> sendEffect(DetailEffect.NavigateToCharacters(malId))

            is DetailEvent.OnCharacterClick -> sendEffect(DetailEffect.NavigateToCharacterDetail(event.characterId))

            is DetailEvent.OnStaffMemberClick -> sendEffect(DetailEffect.NavigateToPersonDetail(event.personMalId))

            DetailEvent.OnSeeAllReviewsClick -> sendEffect(DetailEffect.NavigateToReviews(malId))

            is DetailEvent.OnReviewClick -> {
                setState { copy(selectedReview = event.review) }
                sendEffect(DetailEffect.NavigateToReviewDetail)
            }

            is DetailEvent.OnDownloadPictureClick -> sendEffect(DetailEffect.DownloadPicture(event.url))
        }
    }

    private fun observeCachedDetail() {
        viewModelScope.launch {
            repository.observeAnimeDetail(malId).collect { detail ->
                if (detail != null) {
                    setState { copy(detail = detail) }
                }
            }
        }
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            repository.observeCharacters(malId).collect { characters ->
                setState { copy(characters = characters) }
            }
        }
    }

    private fun observeStaff() {
        viewModelScope.launch {
            repository.observeStaff(malId).collect { staff ->
                setState { copy(staff = staff) }
            }
        }
    }

    private fun observeRecommendations() {
        viewModelScope.launch {
            repository.observeRecommendations(malId).collect { recommendations ->
                setState { copy(recommendations = recommendations) }
            }
        }
    }

    private fun observeReviewPreview() {
        viewModelScope.launch {
            repository.observeReviewPreview(malId).collect { reviews ->
                setState { copy(reviews = reviews) }
            }
        }
    }

    private fun observePictures() {
        viewModelScope.launch {
            repository.observePictures(malId).collect { pictures ->
                setState { copy(pictures = pictures) }
            }
        }
    }

    private fun observeThemes() {
        viewModelScope.launch {
            repository.observeThemes(malId).collect { themes ->
                setState { copy(themes = themes) }
            }
        }
    }

    private fun observeStreamingLinks() {
        viewModelScope.launch {
            repository.observeStreamingLinks(malId).collect { links ->
                setState { copy(streamingLinks = links) }
            }
        }
    }

    private fun observeVideos() {
        viewModelScope.launch {
            repository.observeVideos(malId).collect { videos ->
                setState { copy(videos = videos) }
            }
        }
    }

    private fun observeFavoriteStatus() {
        viewModelScope.launch {
            favoriteRepository.observeIsFavorite(malId).collect { isFavorite ->
                setState { copy(isFavorite = isFavorite) }
            }
        }
    }

    private suspend fun loadAll(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }

        val detailResult = loadMutex.withLock { repository.refreshAnimeDetail(malId, force) }
        when (detailResult) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                // Đã có cache thì giữ nguyên hiển thị, refresh lỗi không chặn
                // màn hình (stale-while-revalidate) — chỉ báo full error khi
                // chưa từng có dữ liệu để hiện.
                val hasCachedDetail = currentState().detail != null
                setState {
                    copy(
                        isLoading = false,
                        error = if (hasCachedDetail) null else detailResult.error.toUserMessage(),
                    )
                }
                if (!hasCachedDetail) return
            }
        }

        // Streaming links refresh NGAY SAU detail (trước cả episodes) — thứ tự
        // chuỗi tuần tự khớp thứ tự HIỂN THỊ: StreamingLinksRow render gần đầu
        // màn (dưới GenreChips), nếu để cuối chuỗi (như bản đầu) thì vùng user
        // nhìn thấy ngay lại có data về muộn nhất (~4-8s cold cache) và row
        // "mọc" ra đẩy layout đúng lúc đang đọc (phát hiện qua review, sửa).
        loadMutex.withLock { repository.refreshStreamingLinks(malId, force) }

        // Episodes KHÔNG cache — luôn gọi lại, set thẳng ở đây (không qua Flow
        // như các mục dưới vì không có Room đứng giữa).
        val episodesResult = loadMutex.withLock { repository.getEpisodes(malId) }
        if (episodesResult is ApiResult.Success) {
            setState { copy(episodes = episodesResult.data) }
        }

        // Characters/staff/recommendations/reviews/pictures/themes/videos:
        // không setState thủ công — observeXxx() (Flow từ Room) trong init đã
        // tự cập nhật state reactively khi refresh xong. refreshXxx tự quyết
        // định gọi API hay không dựa TTL (force=true khi pull-to-refresh sẽ bỏ
        // qua TTL). Videos để CUỐI vì tab Video nằm đáy trang (cùng logic thứ
        // tự-theo-hiển-thị với streaming ở trên). "Thống kê" ĐÃ CHUYỂN sang
        // ReviewsViewModel (màn Đánh giá "Xem tất cả") — không còn refresh ở
        // đây, xem docs/ROADMAP.md. Staff (MVP5) đặt ngay sau Characters vì
        // section "Ê-kíp sản xuất" render ngay sau "Nhân vật & Seiyuu".
        loadMutex.withLock { repository.refreshCharacters(malId, force) }
        loadMutex.withLock { repository.refreshStaff(malId, force) }
        loadMutex.withLock { repository.refreshRecommendations(malId, force) }
        loadMutex.withLock { repository.refreshReviewPreview(malId, force) }
        loadMutex.withLock { repository.refreshPictures(malId, force) }
        loadMutex.withLock { repository.refreshThemes(malId, force) }
        loadMutex.withLock { repository.refreshVideos(malId, force) }
    }
}
