package com.lambao.animike.ui.home

import com.lambao.animike.domain.model.Anime

data class HomeState(
    val isLoading: Boolean = true,
    val animeList: List<Anime> = emptyList(),
    val error: String? = null,
)

sealed interface HomeEvent {
    data object OnRetry : HomeEvent
}

// Chưa có hiệu ứng one-shot nào ở slice này (chưa có navigation/snackbar) —
// khai báo rỗng để giữ đúng khuôn mẫu State/Event/Effect cho các screen sau.
sealed interface HomeEffect
