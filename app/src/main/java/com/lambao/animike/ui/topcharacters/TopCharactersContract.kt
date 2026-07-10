package com.lambao.animike.ui.topcharacters

// Toàn bộ dữ liệu là Flow<PagingData> (không có state header như StudioDetail)
// nên State rỗng — giống CommunityRecommendationsState (data object).
data object TopCharactersState

sealed interface TopCharactersEvent {
    data class OnCharacterClick(val characterId: Int) : TopCharactersEvent
}

sealed interface TopCharactersEffect {
    data class NavigateToCharacterDetail(val characterId: Int) : TopCharactersEffect
}
