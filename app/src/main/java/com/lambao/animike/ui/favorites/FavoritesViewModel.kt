package com.lambao.animike.ui.favorites

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoriteRepository,
) : BaseViewModel<FavoritesState, FavoritesEvent, FavoritesEffect>(FavoritesState()) {

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { favorites ->
                setState { copy(favorites = favorites) }
            }
        }
    }

    override fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.OnAnimeClick -> sendEffect(FavoritesEffect.NavigateToDetail(event.malId))
        }
    }
}
