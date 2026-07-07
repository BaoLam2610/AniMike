package com.lambao.animike.ui.persondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.ApiResult
import com.lambao.animike.data.repository.PersonDetailRepository
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PersonDetailRepository,
) : BaseViewModel<PersonDetailState, PersonDetailEvent, PersonDetailEffect>(PersonDetailState()) {

    private val personId: Int = checkNotNull(savedStateHandle[Routes.PERSON_DETAIL_ARG_PERSON_ID])
    private var loadJob: Job? = null

    init {
        // Room là nguồn hiển thị duy nhất — cùng pattern CharacterDetailViewModel.
        observePersonDetail()
        observeStaffCredits()
        observeVoiceRoles()
        loadJob = viewModelScope.launch { load() }
    }

    override fun onEvent(event: PersonDetailEvent) {
        when (event) {
            PersonDetailEvent.OnRetry -> {
                val previous = loadJob
                loadJob = viewModelScope.launch {
                    previous?.cancelAndJoin()
                    load(force = true)
                }
            }

            is PersonDetailEvent.OnAnimeClick -> sendEffect(PersonDetailEffect.NavigateToDetail(event.malId))

            is PersonDetailEvent.OnVoiceSearchQueryChange -> setState { copy(voiceSearchQuery = event.query) }
        }
    }

    private fun observePersonDetail() {
        viewModelScope.launch {
            repository.observePersonDetail(personId).collect { person ->
                setState { copy(person = person) }
            }
        }
    }

    private fun observeStaffCredits() {
        viewModelScope.launch {
            repository.observeStaffCredits(personId).collect { credits ->
                setState { copy(staffCredits = credits) }
            }
        }
    }

    private fun observeVoiceRoles() {
        viewModelScope.launch {
            repository.observeVoiceRoles(personId).collect { roles ->
                setState { copy(voiceRoles = roles) }
            }
        }
    }

    private suspend fun load(force: Boolean = false) {
        setState { copy(isLoading = true, error = null) }
        when (val result = repository.refreshPersonDetail(personId, force)) {
            is ApiResult.Success -> setState { copy(isLoading = false) }
            is ApiResult.Error -> {
                val hasCached = currentState().person != null
                setState {
                    copy(isLoading = false, error = if (hasCached) null else result.error.toUserMessage())
                }
            }
        }
    }
}
