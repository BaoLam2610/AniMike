package com.lambao.animike.ui.studiodetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.StudioDetailRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Page size mặc định của /anime search (25/trang) — verify qua references.
private const val PAGE_SIZE = 25

@HiltViewModel
class StudioDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StudioDetailRepository,
) : BaseViewModel<StudioDetailState, StudioDetailEvent, StudioDetailEffect>(StudioDetailState()) {

    private val studioId: Int = checkNotNull(savedStateHandle[Routes.STUDIO_DETAIL_ARG_STUDIO_ID])
    private var loadJob: Job? = null

    // Danh sách anime studio sản xuất — Paging 3 RIÊNG với state header
    // (giống ReviewsViewModel.items tách khỏi statistics).
    val items: Flow<PagingData<Anime>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.studioAnimePagingSource(studioId) },
    ).flow.cachedIn(viewModelScope)

    init {
        observeStudioDetail()
        loadJob = viewModelScope.launch { load() }
    }

    override fun onEvent(event: StudioDetailEvent) {
        when (event) {
            StudioDetailEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    load(force = true)
                }
            }

            is StudioDetailEvent.OnAnimeClick -> sendEffect(StudioDetailEffect.NavigateToDetail(event.malId))

            is StudioDetailEvent.OnExternalLinkClick -> sendEffect(StudioDetailEffect.OpenExternalUrl(event.url))
        }
    }

    private fun observeStudioDetail() {
        viewModelScope.launch {
            repository.observeStudioDetail(studioId).collect { studio ->
                setState { copy(studio = studio) }
            }
        }
    }

    private suspend fun load(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.refreshStudioDetail(studioId, force)) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                val hasCached = currentState().studio != null
                setState {
                    copy(isLoading = false, error = if (hasCached) null else result.error.toUserMessage())
                }
            }
        }
    }
}
