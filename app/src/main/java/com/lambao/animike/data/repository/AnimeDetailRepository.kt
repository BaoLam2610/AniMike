package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.AnimeStatisticsDao
import com.lambao.animike.data.local.dao.AnimeThemesDao
import com.lambao.animike.data.local.dao.CharacterDao
import com.lambao.animike.data.local.dao.PictureDao
import com.lambao.animike.data.local.dao.ReviewPreviewDao
import com.lambao.animike.data.local.entity.AnimeListKey
import com.lambao.animike.data.local.entity.CachedCharacterEntity
import com.lambao.animike.data.local.entity.CachedPictureEntity
import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toCharacterEntity
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.mapper.toListEntity
import com.lambao.animike.domain.mapper.toPictureEntity
import com.lambao.animike.domain.mapper.toPreviewEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.AnimeStatistics
import com.lambao.animike.domain.model.AnimeThemes
import com.lambao.animike.domain.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AnimeDetailRepository {
    fun observeAnimeDetail(malId: Int): Flow<AnimeDetail?>
    suspend fun refreshAnimeDetail(malId: Int, force: Boolean = false): ApiResult<Unit>

    // SWR giống Recommendations/Pictures (Room cache riêng, TTL dài) — Detail
    // preview và CharactersScreen ("Xem tất cả") cùng đọc 1 bảng nên không gọi
    // trùng /characters khi user bấm "Xem tất cả" ngay sau khi Detail vừa tải.
    fun observeCharacters(malId: Int): Flow<List<AnimeCharacter>>
    suspend fun refreshCharacters(malId: Int, force: Boolean = false): ApiResult<Unit>

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

    // SWR cho "Biểu đồ phân bố điểm" (/statistics) — 1 row/anime giống
    // AnimeDetail, TTL dài (7 ngày) vì số liệu MAL nhích dần chứ không đổi
    // đột ngột.
    fun observeStatistics(malId: Int): Flow<AnimeStatistics?>
    suspend fun refreshStatistics(malId: Int, force: Boolean = false): ApiResult<Unit>

    // SWR cho "Nhạc OP/ED" (/themes) — 1 row/anime, TTL dài (7 ngày) vì OP/ED
    // gần như KHÔNG BAO GIỜ đổi sau khi anime phát sóng xong.
    fun observeThemes(malId: Int): Flow<AnimeThemes?>
    suspend fun refreshThemes(malId: Int, force: Boolean = false): ApiResult<Unit>
}

private const val REVIEWS_LIMIT = 5

class AnimeDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val dao: AnimeDetailDao,
    private val animeListDao: AnimeListDao,
    private val pictureDao: PictureDao,
    private val reviewPreviewDao: ReviewPreviewDao,
    private val characterDao: CharacterDao,
    private val animeStatisticsDao: AnimeStatisticsDao,
    private val animeThemesDao: AnimeThemesDao,
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

    override fun observeCharacters(malId: Int): Flow<List<AnimeCharacter>> =
        // characterId < 0: lọc bỏ sentinel row (xem refreshCharacters).
        characterDao.observe(malId).map { entities ->
            entities.filter { it.characterId >= 0 }.map { it.toDomain() }
        }

    override suspend fun refreshCharacters(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = characterDao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.CHARACTERS_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            // distinctBy: phòng Jikan trả trùng malId nhân vật trong cùng
            // response (cùng lý do với Recommendations/Pictures) — primary
            // key composite (malId, characterId) sẽ REPLACE âm thầm và lệch
            // position nếu không khử trùng trước.
            val characters = api.getCharacters(malId).data.mapNotNull { it.toDomain() }.distinctBy { it.malId }
            val fetchedAt = System.currentTimeMillis()
            val entities = if (characters.isEmpty()) {
                // Sentinel row (characterId=-1, MAL character id thật luôn
                // dương) — cùng lý do với Recommendations/Reviews/Pictures.
                listOf(
                    CachedCharacterEntity(
                        malId = malId,
                        characterId = -1,
                        name = "",
                        imageUrl = null,
                        role = "",
                        voiceActorName = null,
                        position = 0,
                        fetchedAt = fetchedAt,
                    ),
                )
            } else {
                characters.mapIndexed { index, character -> character.toCharacterEntity(malId, index, fetchedAt) }
            }
            characterDao.replace(malId, entities)
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

    override fun observeStatistics(malId: Int): Flow<AnimeStatistics?> =
        animeStatisticsDao.observe(malId).map { it?.toDomain() }

    override suspend fun refreshStatistics(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = animeStatisticsDao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.STATISTICS_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val statistics = api.getAnimeStatistics(malId).data.toDomain()
            animeStatisticsDao.upsert(statistics.toEntity(malId, System.currentTimeMillis()))
        }
    }

    override fun observeThemes(malId: Int): Flow<AnimeThemes?> =
        animeThemesDao.observe(malId).map { it?.toDomain() }

    override suspend fun refreshThemes(malId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = animeThemesDao.getFetchedAt(malId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.THEMES_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val themes = api.getAnimeThemes(malId).data.toDomain()
            animeThemesDao.upsert(themes.toEntity(malId, System.currentTimeMillis()))
        }
    }
}
