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
        // Room là nguồn hiển thị duy nhất — cùng bảng cached_character mà
        // Detail đã refresh, nên nếu user vào đây ngay sau khi Detail tải
        // xong thì đã có sẵn dữ liệu, không cần gọi lại /characters.
        observeCharacters()
        loadJob = viewModelScope.launch { load() }
    }

    override fun onEvent(event: CharactersEvent) {
        when (event) {
            is CharactersEvent.OnQueryChange -> setState { copy(query = event.query) }

            CharactersEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    load(force = true)
                }
            }

            is CharactersEvent.OnCharacterClick -> sendEffect(CharactersEffect.NavigateToCharacterDetail(event.characterId))
        }
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            repository.observeCharacters(malId).collect { characters ->
                setState { copy(allCharacters = characters) }
            }
        }
    }

    private suspend fun load(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.refreshCharacters(malId, force)) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                // Đã có cache thì giữ nguyên hiển thị (stale-while-revalidate),
                // chỉ báo lỗi khi chưa từng có dữ liệu để hiện.
                val hasCached = currentState().allCharacters.isNotEmpty()
                setState {
                    copy(isLoading = false, error = if (hasCached) null else result.error.toUserMessage())
                }
            }
        }
    }
}
