package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.GenreDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SearchRepository {
    fun searchAnimePagingSource(query: String, filters: SearchFilters): PagingSource<Int, Anime>
    fun observeGenres(): Flow<List<Genre>>
    suspend fun refreshGenres(force: Boolean = false): ApiResult<Unit>
}

class SearchRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val genreDao: GenreDao,
) : SearchRepository {

    override fun searchAnimePagingSource(query: String, filters: SearchFilters): PagingSource<Int, Anime> =
        AnimeSearchPagingSource(api, query, filters)

    override fun observeGenres(): Flow<List<Genre>> =
        genreDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshGenres(force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = genreDao.getFetchedAt()
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.GENRE_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val genres = api.getGenres().data.map { it.toDomain() }
            val fetchedAt = System.currentTimeMillis()
            genreDao.replaceAll(genres.map { it.toEntity(fetchedAt) })
        }
    }
}
