package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.PictureDao
import com.lambao.animike.data.local.dao.ReviewPreviewDao
import com.lambao.animike.data.local.entity.AnimeListKey
import com.lambao.animike.data.local.entity.CachedPictureEntity
import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.mapper.toListEntity
import com.lambao.animike.domain.mapper.toPictureEntity
import com.lambao.animike.domain.mapper.toPreviewEntity
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

    // Characters có cache tạm trong bộ nhớ (xem charactersCache ở Impl) vì
    // /characters không phân trang — Detail preview và CharactersScreen ("Xem
    // tất cả") gọi Y HỆT 1 request, cache dedup tránh gọi 2 lần liên tiếp khi
    // user bấm "Xem tất cả" ngay sau đó.
    suspend fun getCharacters(malId: Int): ApiResult<List<AnimeCharacter>>

    // SWR (docs/ROADMAP.md mục 3b) — tái dùng bảng cached_anime_list/AnimeListDao
    // (xem AnimeListKey.detailRecommendations) thay vì thêm entity/DAO riêng,
    // vì shape Anime đã khớp sẵn. TTL dài (7 ngày) vì đề xuất của 1 anime hiếm
    // khi đổi.
    fun observeRecommendations(malId: Int): Flow<List<Anime>>
    suspend fun refreshRecommendations(malId: Int, force: Boolean = false): ApiResult<Unit>

    // "Các tập" KHÔNG cache — luôn gọi lại /videos/episodes mỗi lần vào Detail
    // vì tập mới có thể ra bất cứ lúc nào; cache dễ khiến user tưởng nhầm đã
    // xem hết trong khi thực ra có tập mới (quyết định rõ ràng của user, xem
    // docs/ROADMAP.md mục 3b — KHÁC với Recommendations/Pictures/Reviews).
    suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>>

    // Bản Paging 3 của getEpisodes — dùng cho EpisodesScreen ("Xem tất cả"),
    // cuộn xuống tự gọi thêm trang.
    fun episodesPagingSource(malId: Int): PagingSource<Int, Episode>

    // SWR cho preview "Đánh giá" (page 1, cắt còn REVIEWS_LIMIT review mới
    // nhất) — TTL ngắn hơn Recommendations/Pictures vì user MAL đăng review
    // liên tục bất kể anime đang chiếu hay đã xong.
    fun observeReviewPreview(malId: Int): Flow<List<AnimeReview>>
    suspend fun refreshReviewPreview(malId: Int, force: Boolean = false): ApiResult<Unit>

    // Bản Paging 3 của reviews — dùng cho ReviewsScreen ("Xem tất cả"), cuộn
    // xuống tự gọi thêm trang. KHÔNG liên quan cache Room ở trên.
    fun reviewsPagingSource(malId: Int): PagingSource<Int, AnimeReview>

    // SWR cho bộ sưu tập ảnh (/pictures — FEATURES.md mục 1.3). TTL dài (7
    // ngày) vì poster art của 1 anime gần như tĩnh.
    fun observePictures(malId: Int): Flow<List<String>>
    suspend fun refreshPictures(malId: Int, force: Boolean = false): ApiResult<Unit>
}

private const val REVIEWS_LIMIT = 5

class AnimeDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeDetailDao,
    private val animeListDao: AnimeListDao,
    private val pictureDao: PictureDao,
    private val reviewPreviewDao: ReviewPreviewDao,
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

    override fun observeRecommendations(malId: Int): Flow<List<Anime>> =
        // malId < 0: lọc bỏ sentinel row (xem refreshRecommendations).
        animeListDao.observeList(AnimeListKey.detailRecommendations(malId)).map { entities ->
            entities.filter { it.malId >= 0 }.map { it.toDomain() }
        }

    override suspend fun refreshRecommendations(malId: Int, force: Boolean): ApiResult<Unit> {
        val listKey = AnimeListKey.detailRecommendations(malId)
        if (!force) {
            val fetchedAt = animeListDao.getFetchedAt(listKey)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.RECOMMENDATIONS_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val recommendations = api.getRecommendations(malId).data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
            val fetchedAt = System.currentTimeMillis()
            val entities = if (recommendations.isEmpty()) {
                // getFetchedAt (MIN aggregate) trả null mãi mãi nếu không ghi
                // row nào khi rỗng thật (anime không có đề xuất) — sentinel
                // malId=-1 (không phải mal_id thật) giữ chỗ fetchedAt, lọc bỏ
                // ở observeRecommendations.
                listOf(
                    Anime(malId = -1, title = "", imageUrl = null, score = "N/A", year = null)
                        .toListEntity(listKey, 0, fetchedAt),
                )
            } else {
                recommendations.mapIndexed { index, anime -> anime.toListEntity(listKey, index, fetchedAt) }
            }
            animeListDao.replaceList(listKey, entities)
        }
    }

    override suspend fun getEpisodes(malId: Int): ApiResult<List<Episode>> = safeApiCall {
        api.getEpisodes(malId).data.map { it.toDomain() }
    }

    override fun episodesPagingSource(malId: Int): PagingSource<Int, Episode> =
        AnimeEpisodesPagingSource(api, malId)

    override fun observeReviewPreview(malId: Int): Flow<List<AnimeReview>> =
        // reviewId < 0: lọc bỏ sentinel row (xem refreshReviewPreview).
        reviewPreviewDao.observe(malId).map { entities ->
            entities.filter { it.reviewId >= 0 }.map { it.toDomain() }
        }

    override suspend fun refreshReviewPreview(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = reviewPreviewDao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.REVIEWS_PREVIEW_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val reviews = api.getReviews(malId).data.take(REVIEWS_LIMIT).map { it.toDomain() }
            val fetchedAt = System.currentTimeMillis()
            val entities = if (reviews.isEmpty()) {
                // Sentinel row (reviewId=-1, MAL review id thật luôn dương) —
                // cùng lý do với Recommendations ở trên.
                listOf(
                    CachedReviewPreviewEntity(
                        malId = malId,
                        reviewId = -1,
                        username = "",
                        score = null,
                        reviewText = "",
                        position = 0,
                        fetchedAt = fetchedAt,
                    ),
                )
            } else {
                reviews.mapIndexed { index, review -> review.toPreviewEntity(malId, index, fetchedAt) }
            }
            reviewPreviewDao.replace(malId, entities)
        }
    }

    override fun reviewsPagingSource(malId: Int): PagingSource<Int, AnimeReview> =
        AnimeReviewsPagingSource(api, malId)

    override fun observePictures(malId: Int): Flow<List<String>> =
        // url rỗng: lọc bỏ sentinel row (xem refreshPictures).
        pictureDao.observe(malId).map { entities ->
            entities.filter { it.url.isNotEmpty() }.map { it.toDomain() }
        }

    override suspend fun refreshPictures(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = pictureDao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.PICTURES_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            // distinct(): Jikan đôi khi trả trùng URL trong cùng response —
            // URL cũng là key của LazyRow ở UI nên phải duy nhất. isNotBlank():
            // phòng field trả về chuỗi rỗng thay vì null (Jikan chỉ đảm bảo
            // nullable, không đảm bảo non-blank khi có giá trị) — chuỗi rỗng
            // thật sẽ trùng sentinel bên dưới và bị lọc mất oan nếu không chặn ở đây.
            val urls = api.getPictures(malId).data
                .mapNotNull { it.jpg?.largeImageUrl ?: it.jpg?.imageUrl }
                .filter { it.isNotBlank() }
                .distinct()
            val fetchedAt = System.currentTimeMillis()
            val entities = if (urls.isEmpty()) {
                // Sentinel row (url rỗng, không phải URL thật) — cùng lý do
                // với Recommendations/Reviews ở trên.
                listOf(CachedPictureEntity(malId = malId, url = "", position = 0, fetchedAt = fetchedAt))
            } else {
                urls.mapIndexed { index, url -> url.toPictureEntity(malId, index, fetchedAt) }
            }
            pictureDao.replace(malId, entities)
        }
    }
}
