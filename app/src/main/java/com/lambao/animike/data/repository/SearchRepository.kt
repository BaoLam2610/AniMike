package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters
import javax.inject.Inject

interface SearchRepository {
    fun searchAnimePagingSource(query: String, filters: SearchFilters): PagingSource<Int, Anime>
    suspend fun getGenres(): ApiResult<List<Genre>>
}

class SearchRepositoryImpl @Inject constructor(
    private val api: JikanApi,
) : SearchRepository {

    override fun searchAnimePagingSource(query: String, filters: SearchFilters): PagingSource<Int, Anime> =
        AnimeSearchPagingSource(api, query, filters)

    override suspend fun getGenres(): ApiResult<List<Genre>> = safeApiCall {
        api.getGenres().data.map { it.toDomain() }
    }
}
