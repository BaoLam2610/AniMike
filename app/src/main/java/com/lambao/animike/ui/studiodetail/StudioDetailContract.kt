package com.lambao.animike.ui.studiodetail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.StudioDetail

// state chỉ chứa phần "header" (studio core, SWR từ Room) — danh sách anime
// đi qua Flow<PagingData> RIÊNG trong ViewModel (giống ReviewsScreen: header
// độc lập với loadState của Paging).
@Immutable
data class StudioDetailState(
    val isLoading: Boolean = true,
    val studio: StudioDetail? = null,
    val error: String? = null,
)

sealed interface StudioDetailEvent {
    data object OnRetry : StudioDetailEvent
    data class OnAnimeClick(val malId: Int) : StudioDetailEvent
    data class OnExternalLinkClick(val url: String) : StudioDetailEvent
}

sealed interface StudioDetailEffect {
    data class NavigateToDetail(val malId: Int) : StudioDetailEffect
    data class OpenExternalUrl(val url: String) : StudioDetailEffect
}
