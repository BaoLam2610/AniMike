package com.lambao.animike.ui.characterdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.CharacterDetailRepository
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CharacterDetailRepository,
) : BaseViewModel<CharacterDetailState, CharacterDetailEvent, CharacterDetailEffect>(CharacterDetailState()) {

    private val characterId: Int = checkNotNull(savedStateHandle[Routes.CHARACTER_DETAIL_ARG_CHARACTER_ID])
    private var loadJob: Job? = null

    init {
        // Room là nguồn hiển thị duy nhất — cùng pattern DetailViewModel: collect
        // Flow suốt vòng đời màn hình, loadAll() chỉ quyết định KHI NÀO gọi API.
        observeCharacterDetail()
        observeAnimeAppearances()
        observeVoiceActors()
        loadJob = viewModelScope.launch { load() }
    }

    override fun onEvent(event: CharacterDetailEvent) {
        when (event) {
            CharacterDetailEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    load(force = true)
                }
            }

            is CharacterDetailEvent.OnAnimeClick -> sendEffect(CharacterDetailEffect.NavigateToDetail(event.malId))

            is CharacterDetailEvent.OnVoiceActorClick ->
                sendEffect(CharacterDetailEffect.NavigateToPersonDetail(event.personMalId))
        }
    }

    private fun observeCharacterDetail() {
        viewModelScope.launch {
            repository.observeCharacterDetail(characterId).collect { character ->
                setState { copy(character = character) }
            }
        }
    }

    private fun observeAnimeAppearances() {
        viewModelScope.launch {
            repository.observeAnimeAppearances(characterId).collect { appearances ->
                setState { copy(animeAppearances = appearances) }
            }
        }
    }

    private fun observeVoiceActors() {
        viewModelScope.launch {
            repository.observeVoiceActors(characterId).collect { actors ->
                setState { copy(voiceActors = actors) }
            }
        }
    }

    private suspend fun load(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.refreshCharacterDetail(characterId, force)) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                // Stale-while-revalidate: đã có cache thì giữ nguyên hiển thị,
                // chỉ báo full error khi chưa từng có dữ liệu để hiện.
                val hasCached = currentState().character != null
                setState {
                    copy(isLoading = false, error = if (hasCached) null else result.error.toUserMessage())
                }
            }
        }
    }
}
