package com.lambao.animike.data.repository

import android.util.Log
import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.CommunityRecommendationDao
import com.lambao.animike.data.local.dao.NewEpisodeDao
import com.lambao.animike.data.local.entity.AnimeListKey
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.mapper.toListEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeListSource
import com.lambao.animike.domain.model.CommunityRecommendation
import com.lambao.animike.domain.model.NewEpisodeRelease
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

    // Bản Paging 3 cho màn "Xem tất cả" (Top Hits / Sắp chiếu) — cuộn xuống
    // tự tải thêm trang, tách khỏi cache Room của 3 list preview phía trên.
    fun animeListPagingSource(source: AnimeListSource): PagingSource<Int, Anime>

    // MVP4 "Hôm nay xem gì?" — không cache (mỗi lần bấm phải thực sự ngẫu
    // nhiên), chỉ cần malId để điều hướng sang Detail.
    suspend fun getRandomAnimeId(): ApiResult<Int>

    // MVP4 "Tập mới phát hành" (/watch/episodes/popular) — SWR như 3 list Home
    // khác, 1 feed toàn cục (không cần listKey riêng theo instance).
    fun observeNewEpisodeReleases(): Flow<List<NewEpisodeRelease>>
    suspend fun refreshNewEpisodeReleases(force: Boolean = false): ApiResult<Unit>

    // MVP4 "Đề xuất cộng đồng" (/recommendations/anime) — preview Home dùng SWR
    // Room (TTL ngắn, giống Reviews vì user đăng liên tục), "Xem tất cả" dùng
    // Paging 3 riêng (endpoint có phân trang THẬT, khác /watch/episodes*).
    fun observeCommunityRecommendations(): Flow<List<CommunityRecommendation>>
    suspend fun refreshCommunityRecommendations(force: Boolean = false): ApiResult<Unit>
    fun communityRecommendationsPagingSource(): PagingSource<Int, CommunityRecommendation>
}

class AnimeRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeListDao,
    private val animeDetailDao: AnimeDetailDao,
    private val newEpisodeDao: NewEpisodeDao,
    private val communityRecommendationDao: CommunityRecommendationDao,
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

    override fun animeListPagingSource(source: AnimeListSource): PagingSource<Int, Anime> =
        AnimeListPagingSource(api, source)

    override suspend fun getRandomAnimeId(): ApiResult<Int> = safeApiCall {
        // /random/anime trả nguyên AnimeFullDto (giống /anime/{id}/full) —
        // ghi thẳng vào cache Detail luôn thay vì chỉ lấy malId rồi vứt phần
        // còn lại. Nếu không, DetailViewModel sẽ cache-miss và gọi lại y hệt
        // /anime/{id}/full ngay sau khi điều hướng — tốn gấp đôi request cho
        // mỗi lần bấm "Hôm nay xem gì?".
        val detail = api.getRandomAnime().data.toDomain()
        animeDetailDao.upsert(detail.toEntity(System.currentTimeMillis()))
        detail.malId
    }

    override fun observeNewEpisodeReleases(): Flow<List<NewEpisodeRelease>> =
        newEpisodeDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshNewEpisodeReleases(force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = newEpisodeDao.getFetchedAt()
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.LIST_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val response = api.getNewEpisodeReleases()
            // Phòng xa: đã verify has_next_page luôn false (endpoint không phân
            // trang thật), nhưng nếu Jikan đổi hành vi sau này thì màn "Xem tất
            // cả" (không Paging 3) sẽ âm thầm thiếu dữ liệu — log để biết sớm
            // thay vì im lặng mãi mãi.
            if (response.pagination?.hasNextPage == true) {
                Log.w(TAG, "/watch/episodes/popular bất ngờ có has_next_page=true — cần xem lại giả định không phân trang")
            }
            val releases = response.data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
            val fetchedAt = System.currentTimeMillis()
            val entities = releases.mapIndexed { index, release -> release.toEntity(index, fetchedAt) }
            newEpisodeDao.replaceAll(entities)
        }
    }

    override fun observeCommunityRecommendations(): Flow<List<CommunityRecommendation>> =
        communityRecommendationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshCommunityRecommendations(force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = communityRecommendationDao.getFetchedAt()
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.COMMUNITY_RECOMMENDATIONS_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            // Chỉ lấy page 1 cho preview Home (giống New Episodes) — "Xem tất
            // cả" dùng communityRecommendationsPagingSource() phân trang riêng,
            // không liên quan cache Room ở đây.
            val recommendations = api.getCommunityRecommendations(page = 1).data
                .mapNotNull { it.toDomain() }
                .distinctBy { it.id }
            val fetchedAt = System.currentTimeMillis()
            val entities = recommendations.mapIndexed { index, item -> item.toEntity(index, fetchedAt) }
            communityRecommendationDao.replaceAll(entities)
        }
    }

    override fun communityRecommendationsPagingSource(): PagingSource<Int, CommunityRecommendation> =
        CommunityRecommendationsPagingSource(api)

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
