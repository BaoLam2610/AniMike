package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.SeasonYearDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.SeasonYear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SeasonArchiveRepository {
    fun observeSeasonsList(): Flow<List<SeasonYear>>
    suspend fun refreshSeasonsList(force: Boolean = false): ApiResult<Unit>
    fun seasonArchivePagingSource(year: Int, season: String): PagingSource<Int, Anime>
}

class SeasonArchiveRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: SeasonYearDao,
) : SeasonArchiveRepository {

    override fun observeSeasonsList(): Flow<List<SeasonYear>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshSeasonsList(force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = dao.getFetchedAt()
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.SEASON_LIST_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val years = api.getSeasonsList().data.map { it.toDomain() }
            val fetchedAt = System.currentTimeMillis()
            dao.replaceAll(years.map { it.toEntity(fetchedAt) })
        }
    }

    override fun seasonArchivePagingSource(year: Int, season: String): PagingSource<Int, Anime> =
        AnimeSeasonArchivePagingSource(api, year, season)
}
