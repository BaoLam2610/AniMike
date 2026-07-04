package com.lambao.animike.ui.search

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.SearchRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.SearchFilters
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 500L
private const val PAGE_SIZE = 25

/**
 * PagingData không đặt trong UiState StateFlow (xem compose-expert/references/
 * paging-mvi-testing.md) — expose riêng `items` bên cạnh state/effect kế thừa
 * từ BaseViewModel.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
) : BaseViewModel<SearchState, SearchEvent, SearchEffect>(SearchState()) {

    private val queryFlow = MutableStateFlow("")
    private val filtersFlow = MutableStateFlow(SearchFilters())

    val items: Flow<PagingData<Anime>> = combine(
        // filtersFlow là StateFlow — đã tự distinct theo equals(), không cần
        // gọi distinctUntilChanged() thêm (chỉ query cần vì debounce() làm nó
        // thành Flow thường, mất bảo đảm distinct sẵn có của StateFlow).
        queryFlow.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
        filtersFlow,
    ) { query, filters -> query to filters }
        .flatMapLatest { (query, filters) ->
            // Query rỗng vẫn gọi API — Jikan trả danh sách chung theo order_by/sort
            // đang chọn (mặc định "Phổ biến"), dùng làm gợi ý duyệt trước khi gõ.
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = { repository.searchAnimePagingSource(query, filters) },
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        // Room là nguồn hiển thị duy nhất cho danh sách thể loại; refresh chỉ
        // quyết định khi nào gọi lại API (TTL 7 ngày — genres gần như tĩnh).
        observeCachedGenres()
        viewModelScope.launch { repository.refreshGenres() }
    }

    override fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> {
                setState { copy(query = event.query) }
                queryFlow.value = event.query
            }

            is SearchEvent.OnTypeFilterChange -> updateFilters { copy(type = event.type) }
            is SearchEvent.OnStatusFilterChange -> updateFilters { copy(status = event.status) }
            is SearchEvent.OnGenreToggle -> updateFilters {
                copy(genreIds = if (event.genreId in genreIds) genreIds - event.genreId else genreIds + event.genreId)
            }

            is SearchEvent.OnSortChange -> updateFilters { copy(orderBy = event.orderBy, sort = event.sort) }
            is SearchEvent.OnAnimeClick -> sendEffect(SearchEffect.NavigateToDetail(event.malId))
        }
    }

    private fun observeCachedGenres() {
        viewModelScope.launch {
            repository.observeGenres().collect { genres ->
                setState { copy(genres = genres) }
            }
        }
    }

    private fun updateFilters(reducer: SearchFilters.() -> SearchFilters) {
        val updated = filtersFlow.value.reducer()
        filtersFlow.value = updated
        setState { copy(filters = updated) }
    }
}
