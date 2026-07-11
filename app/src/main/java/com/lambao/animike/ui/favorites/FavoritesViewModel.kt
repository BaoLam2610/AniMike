package com.lambao.animike.ui.favorites

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.data.repository.TrackingRepository
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoriteRepository: FavoriteRepository,
    trackingRepository: TrackingRepository,
) : BaseViewModel<FavoritesState, FavoritesEvent, FavoritesEffect>(FavoritesState()) {

    init {
        // Hợp nhất 2 nguồn local (favorites + tracking) thành 1 danh sách
        // LibraryEntry — favorites giữ thứ tự addedAt DESC sẵn có, anime chỉ
        // có tracking (không favorite) nối sau theo updatedAt DESC.
        viewModelScope.launch {
            combine(
                favoriteRepository.observeFavorites(),
                trackingRepository.observeAll(),
            ) { favorites, tracked ->
                val statusByMalId = tracked.associateBy({ it.anime.malId }, { it.status })
                val favoriteIds = favorites.mapTo(mutableSetOf()) { it.malId }
                favorites.map { anime ->
                    LibraryEntry(anime = anime, isFavorite = true, status = statusByMalId[anime.malId])
                } + tracked
                    .filter { it.anime.malId !in favoriteIds }
                    .map { LibraryEntry(anime = it.anime, isFavorite = false, status = it.status) }
            }.collect { entries ->
                // Hợp thức hoá filter khi dữ liệu đổi: chip đang chọn có thể
                // biến mất khỏi row (VD bỏ trạng thái của anime "Đang xem"
                // cuối cùng từ Detail rồi quay lại) — quay về "Tất cả" thay vì
                // kẹt ở filter không còn chip nào highlight (phát hiện qua
                // review). Logic hợp thức hoá thuộc ViewModel, không phải
                // composable (MVI).
                setState {
                    val updated = copy(entries = entries)
                    if (updated.filter == LibraryFilter.All || updated.count(updated.filter) > 0) {
                        updated
                    } else {
                        updated.copy(filter = LibraryFilter.All)
                    }
                }
            }
        }
    }

    override fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.OnAnimeClick -> sendEffect(FavoritesEffect.NavigateToDetail(event.malId))

            is FavoritesEvent.OnFilterSelected -> setState { copy(filter = event.filter) }
        }
    }
}
