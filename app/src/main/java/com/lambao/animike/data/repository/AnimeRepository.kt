package com.lambao.animike.data.repository

import android.util.Log
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import javax.inject.Inject

private const val TAG = "AnimeRepository"

interface AnimeRepository {
    suspend fun getSeasonNow(): ApiResult<List<Anime>>
    suspend fun getTopAnime(): ApiResult<List<Anime>>
    suspend fun getUpcoming(): ApiResult<List<Anime>>
}

class AnimeRepositoryImpl @Inject constructor(
    private val api: JikanApi,
) : AnimeRepository {

    override suspend fun getSeasonNow(): ApiResult<List<Anime>> =
        fetchAnimeList { api.getSeasonNow() }

    override suspend fun getTopAnime(): ApiResult<List<Anime>> =
        fetchAnimeList { api.getTopAnime() }

    override suspend fun getUpcoming(): ApiResult<List<Anime>> =
        fetchAnimeList { api.getUpcoming() }

    private suspend fun fetchAnimeList(
        call: suspend () -> JikanListResponse<AnimeDto>,
    ): ApiResult<List<Anime>> = safeApiCall {
        // Jikan đôi khi trả trùng mal_id trong cùng response (data quirk của
        // MAL) — khử trùng ở đây để domain model luôn có malId duy nhất,
        // tránh vỡ key trong LazyRow/LazyColumn ở UI.
        val mapped = call().data.map { it.toDomain() }
        val deduped = mapped.distinctBy { it.malId }
        if (deduped.size != mapped.size) {
            Log.w(TAG, "Jikan trả ${mapped.size - deduped.size} mal_id trùng lặp trong 1 response")
        }
        deduped
    }
}
