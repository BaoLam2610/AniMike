package com.lambao.animike.ui.communityrecommendations

// PagingData không đặt trong state (compose-expert/references/paging-mvi-
// testing.md) — expose riêng `items` bên cạnh (cùng pattern ReviewsScreen),
// không có filter/selection nên state rỗng.
data object CommunityRecommendationsState

sealed interface CommunityRecommendationsEvent {
    data class OnAnimeClick(val malId: Int) : CommunityRecommendationsEvent
}

sealed interface CommunityRecommendationsEffect {
    data class NavigateToDetail(val malId: Int) : CommunityRecommendationsEffect
}
