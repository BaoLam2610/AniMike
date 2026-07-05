package com.lambao.animike.ui.reviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Khớp page size thật của /reviews — đã verify qua curl: luôn trả 20 item/
// trang bất kể có truyền limit hay không (Jikan bỏ qua query limit ở endpoint này).
private const val PAGE_SIZE = 20

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AnimeDetailRepository,
) : BaseViewModel<ReviewsState, ReviewsEvent, ReviewsEffect>(ReviewsState()) {

    private val malId: Int = checkNotNull(savedStateHandle[Routes.REVIEWS_ARG_MAL_ID])

    val items: Flow<PagingData<AnimeReview>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.reviewsPagingSource(malId) },
    ).flow.cachedIn(viewModelScope)

    init {
        // Room là nguồn hiển thị duy nhất cho statistics — CHUYỂN quyền
        // observe/refresh từ DetailViewModel sang đây (theo yêu cầu user, xem
        // docs/ROADMAP.md). Cache Room (cached_anime_statistics) không đổi,
        // chỉ đổi nơi tiêu thụ.
        viewModelScope.launch {
            repository.observeStatistics(malId).collect { statistics ->
                setState { copy(statistics = statistics) }
            }
        }
        viewModelScope.launch { repository.refreshStatistics(malId) }
    }

    override fun onEvent(event: ReviewsEvent) {
        when (event) {
            is ReviewsEvent.OnReviewClick -> {
                setState { copy(selectedReview = event.review) }
                sendEffect(ReviewsEffect.NavigateToReviewDetail)
            }
        }
    }
}
