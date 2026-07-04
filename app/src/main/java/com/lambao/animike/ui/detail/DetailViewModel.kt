package com.lambao.animike.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.data.repository.ApiResult
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
) : BaseViewModel<DetailState, DetailEvent, DetailEffect>(DetailState()) {

    private val malId: Int = checkNotNull(savedStateHandle[Routes.DETAIL_ARG_MAL_ID])

    // Cùng lý do với HomeViewModel: đảm bảo 3 lệnh gọi (full/characters/
    // recommendations) không bao giờ chạy song song, kể cả khi retry xen vào.
    private val loadMutex = Mutex()
    private var loadJob: Job? = null

    init {
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
                    loadAll()
                }
            }

            DetailEvent.OnTrailerClick -> {
                currentState().detail?.trailerYoutubeId?.let { sendEffect(DetailEffect.OpenYoutube(it)) }
            }

            is DetailEvent.OnRecommendationClick -> sendEffect(DetailEffect.NavigateToDetail(event.malId))
        }
    }

    private suspend fun loadAll() {
        setState { copy(isLoading = true, error = null) }

        val detailResult = loadMutex.withLock { repository.getAnimeDetail(malId) }
        when (detailResult) {
            is ApiResult.Success -> setState { copy(isLoading = false, detail = detailResult.data) }
            is ApiResult.Error -> {
                setState { copy(isLoading = false, error = detailResult.error.toUserMessage()) }
                return
            }
        }

        // Nhân vật & đề xuất không critical — lỗi thì section tương ứng để trống.
        val charactersResult = loadMutex.withLock { repository.getCharacters(malId) }
        if (charactersResult is ApiResult.Success) {
            setState { copy(characters = charactersResult.data) }
        }

        val recommendationsResult = loadMutex.withLock { repository.getRecommendations(malId) }
        if (recommendationsResult is ApiResult.Success) {
            setState { copy(recommendations = recommendationsResult.data) }
        }
    }
}
