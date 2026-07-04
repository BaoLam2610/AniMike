package com.lambao.animike.ui.characters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@HiltViewModel
class CharactersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AnimeDetailRepository,
) : BaseViewModel<CharactersState, CharactersEvent, CharactersEffect>(CharactersState()) {

    private val malId: Int = checkNotNull(savedStateHandle[Routes.CHARACTERS_ARG_MAL_ID])
    private var loadJob: Job? = null

    init {
        loadJob = viewModelScope.launch { load() }
    }

    override fun onEvent(event: CharactersEvent) {
        when (event) {
            is CharactersEvent.OnQueryChange -> setState { copy(query = event.query) }

            CharactersEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    load()
                }
            }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.getCharacters(malId)) {
            is ApiResult.Success -> setState { copy(isLoading = false, allCharacters = result.data) }
            is ApiResult.Error -> setState { copy(isLoading = false, error = result.error.toUserMessage()) }
        }
    }
}
