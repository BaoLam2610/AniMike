package com.lambao.animike.ui.episodes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.domain.model.Episode
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Khớp page size thật của /videos/episodes — đã verify qua curl: luôn trả 40
// item/trang bất kể có truyền limit hay không (Jikan bỏ qua query limit ở
// endpoint này).
private const val PAGE_SIZE = 40

@HiltViewModel
class EpisodesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AnimeDetailRepository,
) : BaseViewModel<EpisodesState, EpisodesEvent, EpisodesEffect>(EpisodesState) {

    private val malId: Int = checkNotNull(savedStateHandle[Routes.EPISODES_ARG_MAL_ID])

    val items: Flow<PagingData<Episode>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.episodesPagingSource(malId) },
    ).flow.cachedIn(viewModelScope)

    // EpisodesEvent rỗng (xem EpisodesContract) — không có nhánh nào để xử lý.
    override fun onEvent(event: EpisodesEvent) = Unit
}
