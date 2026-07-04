package com.lambao.animike.data.repository

import androidx.paging.PagingSource
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

interface AnimeDetailRepository {
    fun observeAnimeDetail(malId: Int): Flow<AnimeDetail?>
    suspend fun refreshAnimeDetail(malId: Int, force: Boolean = false): ApiResult<Unit>

    // Recommendations/episodes/reviews không cache — dữ liệu phụ, refetch rẻ
    // hơn là thêm entity/DAO cho chúng (xem docs/ROADMAP.md mục 3b cho kế
    // hoạch cache Room đầy đủ sau này). Characters có cache tạm trong bộ nhớ
    // (xem charactersCache ở Impl) vì /characters không phân trang — Detail
    // preview và CharactersScreen ("Xem tất cả") gọi Y HỆT 1 request, cache
    // dedup tránh gọi 2 lần liên tiếp khi user bấm "Xem tất cả" ngay sau đó.
    suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>>
    suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>>

    // One-shot page 1 (40 tập mới nhất, đã sắp mới→cũ) — chỉ dùng cho preview
    // 10 tập đầu trong Detail. Gọi /videos/episodes (xem JikanApi) nên chỉ phủ
    // tập có video/promo — anime rất dài có thể thiếu vài tập đầu quá cũ.
    suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>>

    // Bản Paging 3 của getEpisodes — dùng cho EpisodesScreen ("Xem tất cả"),
    // cuộn xuống tự gọi thêm trang. Cùng endpoint/giới hạn coverage như trên.
    fun episodesPagingSource(malId: Int): PagingSource<Int, Episode>

    // One-shot page 1, cắt còn REVIEWS_LIMIT review đầu (đánh giá mới nhất) —
    // chỉ dùng cho preview trong Detail. Cắt ngay ở đây (không phải ở UI) để
    // state chỉ giữ đúng phần dữ liệu thực sự hiển thị.
    suspend fun getReviews(malId: Int): ApiResult<List<AnimeReview>>

    // Bản Paging 3 của getReviews — dùng cho ReviewsScreen ("Xem tất cả"),
    // cuộn xuống tự gọi thêm trang.
    fun reviewsPagingSource(malId: Int): PagingSource<Int, AnimeReview>

    // Bộ sưu tập ảnh (/pictures — FEATURES.md mục 1.3), trả list URL ưu tiên
    // bản large. Không cache, cùng lý do với recommendations (dữ liệu phụ,
    // refetch rẻ hơn thêm entity/DAO — xem docs/ROADMAP.md mục 3b).
    suspend fun getPictures(malId: Int): ApiResult<List<String>>
}

private const val REVIEWS_LIMIT = 5

class AnimeDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeDetailDao,
) : AnimeDetailRepository {

    // Cache tạm trong tiến trình (KHÔNG phải Room, mất khi app bị kill) — chỉ
    // để khử trùng 2 lệnh gọi getCharacters() giống hệt nhau khi Detail vừa
    // fetch xong rồi user bấm "Xem tất cả" ngay sau đó. @Singleton nên field
    // này sống suốt vòng đời app; ConcurrentHashMap vì có thể đọc/ghi từ
    // nhiều ViewModel (Detail + Characters) gần như đồng thời.
    private val charactersCache = ConcurrentHashMap<Int, Pair<Long, List<AnimeCharacter>>>()

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

    override suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>> {
        charactersCache[malId]?.let { (fetchedAt, cached) ->
            if (!isExpired(fetchedAt, CacheTtl.MEMORY_DEDUP_MS)) return ApiResult.Success(cached)
        }
        return safeApiCall {
            api.getCharacters(malId).data.mapNotNull { it.toDomain() }
        }.also { result ->
            if (result is ApiResult.Success) {
                charactersCache[malId] = System.currentTimeMillis() to result.data
            }
        }
    }

    override suspend fun getRecommendations(malId: Int): ApiResult<List<Anime>> = safeApiCall {
        api.getRecommendations(malId).data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
    }

    override suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>> = safeApiCall {
        api.getEpisodes(malId).data.map { it.toDomain() }
    }

    override fun episodesPagingSource(malId: Int): PagingSource<Int, Episode> =
        AnimeEpisodesPagingSource(api, malId)

    override suspend fun getReviews(malId: Int): ApiResult<List<AnimeReview>> = safeApiCall {
        api.getReviews(malId).data.take(REVIEWS_LIMIT).map { it.toDomain() }
    }

    override fun reviewsPagingSource(malId: Int): PagingSource<Int, AnimeReview> =
        AnimeReviewsPagingSource(api, malId)

    override suspend fun getPictures(malId: Int): ApiResult<List<String>> = safeApiCall {
        // distinct(): Jikan đôi khi trả trùng URL trong cùng response —
        // URL cũng là key của LazyRow ở UI nên phải duy nhất.
        api.getPictures(malId).data
            .mapNotNull { it.jpg?.largeImageUrl ?: it.jpg?.imageUrl }
            .distinct()
    }
}
