package com.lambao.animike.ui.characters

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.AnimeCharacter

@Immutable
data class CharactersState(
    val isLoading: Boolean = true,
    val allCharacters: List<AnimeCharacter> = emptyList(),
    val query: String = "",
    val error: String? = null,
) {
    // Lọc cục bộ trong bộ nhớ — API /characters trả về TOÀN BỘ nhân vật 1
    // lần (không phân trang), nên search không cần gọi lại API.
    val filteredCharacters: List<AnimeCharacter>
        get() = if (query.isBlank()) {
            allCharacters
        } else {
            allCharacters.filter { it.name.contains(query, ignoreCase = true) }
        }
}

sealed interface CharactersEvent {
    data class OnQueryChange(val query: String) : CharactersEvent
    data object OnRetry : CharactersEvent
}

sealed interface CharactersEffect
