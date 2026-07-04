package com.lambao.animike.data.repository

import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import javax.inject.Inject

interface AnimeDetailRepository {
    suspend fun getAnimeDetail(malId: Int): ApiResult<AnimeDetail>
    suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>>
    suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>>
}

class AnimeDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
) : AnimeDetailRepository {

    override suspend fun getAnimeDetail(malId: Int): ApiResult<AnimeDetail> = safeApiCall {
        api.getAnimeFull(malId).data.toDomain()
    }

    override suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>> = safeApiCall {
        api.getCharacters(malId).data.mapNotNull { it.toDomain() }
    }

    override suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>> = safeApiCall {
        api.getRecommendations(malId).data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
    }
}
