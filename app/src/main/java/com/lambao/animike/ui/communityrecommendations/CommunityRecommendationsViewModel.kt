package com.lambao.animike.ui.communityrecommendations

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.domain.model.CommunityRecommendation
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Khớp page size thật của /recommendations/anime — đã verify qua curl: luôn
// trả 100 item/trang.
private const val PAGE_SIZE = 100

@HiltViewModel
class CommunityRecommendationsViewModel @Inject constructor(
    repository: AnimeRepository,
) : BaseViewModel<CommunityRecommendationsState, CommunityRecommendationsEvent, CommunityRecommendationsEffect>(
    CommunityRecommendationsState,
) {

    val items: Flow<PagingData<CommunityRecommendation>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.communityRecommendationsPagingSource() },
    ).flow.cachedIn(viewModelScope)

    override fun onEvent(event: CommunityRecommendationsEvent) {
        when (event) {
            is CommunityRecommendationsEvent.OnAnimeClick ->
                sendEffect(CommunityRecommendationsEffect.NavigateToDetail(event.malId))
        }
    }
}
