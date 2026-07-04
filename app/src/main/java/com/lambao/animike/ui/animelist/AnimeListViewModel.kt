package com.lambao.animike.ui.animelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeListSource
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Khớp page size mặc định của Jikan /top/anime và /seasons/upcoming (25/trang).
private const val PAGE_SIZE = 25

@HiltViewModel
class AnimeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AnimeRepository,
) : BaseViewModel<AnimeListState, AnimeListEvent, AnimeListEffect>(AnimeListState) {

    // Route arg là enum name (Routes.animeList) — valueOf ném IllegalArgument
    // nếu route bị gõ sai, fail sớm ngay lúc dev thay vì màn trống khó hiểu.
    val source: AnimeListSource = AnimeListSource.valueOf(
        checkNotNull(savedStateHandle[Routes.ANIME_LIST_ARG_SOURCE]),
    )

    val items: Flow<PagingData<Anime>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.animeListPagingSource(source) },
    ).flow.cachedIn(viewModelScope)

    override fun onEvent(event: AnimeListEvent) {
        when (event) {
            is AnimeListEvent.OnAnimeClick -> sendEffect(AnimeListEffect.NavigateToDetail(event.malId))
        }
    }
}
