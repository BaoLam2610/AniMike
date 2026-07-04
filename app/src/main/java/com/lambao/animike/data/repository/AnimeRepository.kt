package com.lambao.animike.data.repository

import android.util.Log
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.entity.AnimeListKey
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toListEntity
import com.lambao.animike.domain.model.Anime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "AnimeRepository"

interface AnimeRepository {
    fun observeSeasonNow(): Flow<List<Anime>>
    fun observeTopAnime(): Flow<List<Anime>>
    fun observeUpcoming(): Flow<List<Anime>>

    suspend fun refreshSeasonNow(force: Boolean = false): ApiResult<Unit>
    suspend fun refreshTopAnime(force: Boolean = false): ApiResult<Unit>
    suspend fun refreshUpcoming(force: Boolean = false): ApiResult<Unit>
}

class AnimeRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeListDao,
) : AnimeRepository {

    override fun observeSeasonNow(): Flow<List<Anime>> = observeList(AnimeListKey.SEASON_NOW)
    override fun observeTopAnime(): Flow<List<Anime>> = observeList(AnimeListKey.TOP_ANIME)
    override fun observeUpcoming(): Flow<List<Anime>> = observeList(AnimeListKey.UPCOMING)

    override suspend fun refreshSeasonNow(force: Boolean): ApiResult<Unit> =
        refreshList(AnimeListKey.SEASON_NOW, force) { api.getSeasonNow() }

    override suspend fun refreshTopAnime(force: Boolean): ApiResult<Unit> =
        refreshList(AnimeListKey.TOP_ANIME, force) { api.getTopAnime() }

    override suspend fun refreshUpcoming(force: Boolean): ApiResult<Unit> =
        refreshList(AnimeListKey.UPCOMING, force) { api.getUpcoming() }

    private fun observeList(listKey: String): Flow<List<Anime>> =
        dao.observeList(listKey).map { entities -> entities.map { it.toDomain() } }

    /** Stale-while-revalidate: hết TTL (hoặc force) mới gọi API, ghi đè theo listKey. */
    private suspend fun refreshList(
        listKey: String,
        force: Boolean,
        call: suspend () -> JikanListResponse<AnimeDto>,
    ): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = dao.getFetchedAt(listKey)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.LIST_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            // Jikan đôi khi trả trùng mal_id trong cùng response (data quirk của
            // MAL) — khử trùng ở đây để domain model luôn có malId duy nhất,
            // tránh vỡ key trong LazyRow/LazyColumn ở UI.
            val mapped = call().data.map { it.toDomain() }
            val deduped = mapped.distinctBy { it.malId }
            if (deduped.size != mapped.size) {
                Log.w(TAG, "Jikan trả ${mapped.size - deduped.size} mal_id trùng lặp trong 1 response")
            }
            val fetchedAt = System.currentTimeMillis()
            val entities = deduped.mapIndexed { index, anime -> anime.toListEntity(listKey, index, fetchedAt) }
            dao.replaceList(listKey, entities)
        }
    }
}
