package com.lambao.animike.ui.favorites

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.WatchStatus

// MVP6 — "Danh sách" nâng từ grid favorites thuần thành THƯ VIỆN: hợp nhất
// anime yêu thích + anime có trạng thái xem (1 anime có thể thuộc cả 2).
@Immutable
data class LibraryEntry(
    val anime: Anime,
    val isFavorite: Boolean,
    val status: WatchStatus?,
)

// Sealed thay vì enum — filter theo trạng thái cần mang theo WatchStatus,
// tránh duplicate 5 hằng số enum chỉ để map 1-1 ngược lại.
sealed interface LibraryFilter {
    data object All : LibraryFilter
    data object Favorite : LibraryFilter
    data class ByStatus(val status: WatchStatus) : LibraryFilter
}

@Immutable
data class FavoritesState(
    val entries: List<LibraryEntry> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
) {
    val filteredEntries: List<LibraryEntry>
        get() = when (val f = filter) {
            LibraryFilter.All -> entries
            LibraryFilter.Favorite -> entries.filter { it.isFavorite }
            is LibraryFilter.ByStatus -> entries.filter { it.status == f.status }
        }

    fun count(filter: LibraryFilter): Int = when (filter) {
        LibraryFilter.All -> entries.size
        LibraryFilter.Favorite -> entries.count { it.isFavorite }
        is LibraryFilter.ByStatus -> entries.count { it.status == filter.status }
    }
}

sealed interface FavoritesEvent {
    data class OnAnimeClick(val malId: Int) : FavoritesEvent
    data class OnFilterSelected(val filter: LibraryFilter) : FavoritesEvent
}

sealed interface FavoritesEffect {
    data class NavigateToDetail(val malId: Int) : FavoritesEffect
}
