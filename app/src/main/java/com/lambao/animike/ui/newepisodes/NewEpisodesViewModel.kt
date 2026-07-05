package com.lambao.animike.ui.newepisodes

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@HiltViewModel
class NewEpisodesViewModel @Inject constructor(
    private val repository: AnimeRepository,
) : BaseViewModel<NewEpisodesState, NewEpisodesEvent, NewEpisodesEffect>(NewEpisodesState()) {

    private var loadJob: Job? = null

    init {
        // Cùng nguồn cache Room với section preview ở Home — nếu Home đã
        // refresh xong trước đó, refresh() ở đây sẽ no-op (còn trong TTL).
        observeCached()
        loadJob = viewModelScope.launch { refresh() }
    }

    override fun onEvent(event: NewEpisodesEvent) {
        when (event) {
            NewEpisodesEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    refresh(force = true)
                }
            }

            is NewEpisodesEvent.OnAnimeClick -> sendEffect(NewEpisodesEffect.NavigateToDetail(event.malId))
        }
    }

    private fun observeCached() {
        viewModelScope.launch {
            repository.observeNewEpisodeReleases().collect { list ->
                setState { copy(releases = list) }
            }
        }
    }

    private suspend fun refresh(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.refreshNewEpisodeReleases(force)) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                // Cache-first: đã có cache thì giữ nguyên hiển thị, chỉ báo
                // full error khi chưa từng có dữ liệu để hiện.
                val hasCache = currentState().releases.isNotEmpty()
                setState {
                    copy(isLoading = false, error = if (hasCache) null else result.error.toUserMessage())
                }
            }
        }
    }
}
