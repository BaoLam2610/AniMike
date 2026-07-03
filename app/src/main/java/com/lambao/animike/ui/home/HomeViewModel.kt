package com.lambao.animike.ui.home

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {

    private var loadJob: Job? = null

    init {
        loadTopAnime()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRetry -> loadTopAnime()
        }
    }

    private fun loadTopAnime() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = repository.getTopAnime()) {
                is ApiResult.Success -> setState {
                    copy(isLoading = false, animeList = result.data)
                }

                is ApiResult.Error -> setState {
                    copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }
}
