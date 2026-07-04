package com.lambao.animike.data.repository

import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AnimeDetailRepository {
    fun observeAnimeDetail(malId: Int): Flow<AnimeDetail?>
    suspend fun refreshAnimeDetail(malId: Int, force: Boolean = false): ApiResult<Unit>

    // Characters/recommendations/episodes/reviews không cache — dữ liệu phụ,
    // refetch rẻ hơn là thêm entity/DAO cho chúng (xem docs/ROADMAP.md mục 3).
    suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>>
    suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>>

    // Chỉ page 1 (100 tập) — đủ cho đa số anime, MVP chưa cần phân trang tập.
    suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>>

    // Chỉ lấy REVIEWS_LIMIT review đầu của page 1 (đánh giá mới nhất) — đủ cho
    // MVP, chưa cần phân trang. Cắt ngay ở đây (không phải ở UI) để state chỉ
    // giữ đúng phần dữ liệu thực sự hiển thị.
    suspend fun getReviews(malId: Int): ApiResult<List<AnimeReview>>
}

private const val REVIEWS_LIMIT = 5

class AnimeDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeDetailDao,
) : AnimeDetailRepository {

    override fun observeAnimeDetail(malId: Int): Flow<AnimeDetail?> =
        dao.observe(malId).map { it?.toDomain() }

    override suspend fun refreshAnimeDetail(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = dao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.DETAIL_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val detail = api.getAnimeFull(malId).data.toDomain()
            dao.upsert(detail.toEntity(System.currentTimeMillis()))
        }
    }

    override suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>> = safeApiCall {
        api.getCharacters(malId).data.mapNotNull { it.toDomain() }
    }

    override suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>> = safeApiCall {
        api.getRecommendations(malId).data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
    }

    override suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>> = safeApiCall {
        api.getEpisodes(malId).data.map { it.toDomain() }
    }

    override suspend fun getReviews(malId: Int): ApiResult<List<AnimeReview>> = safeApiCall {
        api.getReviews(malId).data.take(REVIEWS_LIMIT).map { it.toDomain() }
    }
}
