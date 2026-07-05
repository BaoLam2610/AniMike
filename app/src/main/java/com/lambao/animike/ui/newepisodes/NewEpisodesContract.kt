package com.lambao.animike.ui.newepisodes

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.NewEpisodeRelease

// "Xem tất cả" của "Tập mới phát hành" (MVP4) — không phân trang vì
// /watch/episodes không hỗ trợ (đã verify page=1/page=2 trả y hệt nhau),
// đây chỉ đơn giản là quan sát lại đúng cache mà Home đã refresh, hiển thị
// TOÀN BỘ thay vì cắt còn preview.
@Immutable
data class NewEpisodesState(
    val isLoading: Boolean = true,
    val releases: List<NewEpisodeRelease> = emptyList(),
    val error: String? = null,
)

sealed interface NewEpisodesEvent {
    data object OnRetry : NewEpisodesEvent
    data class OnAnimeClick(val malId: Int) : NewEpisodesEvent
}

sealed interface NewEpisodesEffect {
    data class NavigateToDetail(val malId: Int) : NewEpisodesEffect
}
