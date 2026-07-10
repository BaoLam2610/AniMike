package com.lambao.animike.ui.topcharacters

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.domain.model.TopCharacter
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Khớp page size thật của /top/characters — verify: 25 item/trang.
private const val PAGE_SIZE = 25

@HiltViewModel
class TopCharactersViewModel @Inject constructor(
    repository: AnimeRepository,
) : BaseViewModel<TopCharactersState, TopCharactersEvent, TopCharactersEffect>(TopCharactersState) {

    val items: Flow<PagingData<TopCharacter>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { repository.topCharactersPagingSource() },
    ).flow.cachedIn(viewModelScope)

    override fun onEvent(event: TopCharactersEvent) {
        when (event) {
            is TopCharactersEvent.OnCharacterClick ->
                sendEffect(TopCharactersEffect.NavigateToCharacterDetail(event.characterId))
        }
    }
}
