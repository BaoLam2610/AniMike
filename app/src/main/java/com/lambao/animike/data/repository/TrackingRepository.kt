package com.lambao.animike.data.repository

import com.lambao.animike.data.local.dao.TrackingDao
import com.lambao.animike.debug.AppLog
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toTrackingEntity
import com.lambao.animike.domain.mapper.toTrackingSnapshot
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.TrackedAnime
import com.lambao.animike.domain.model.WatchStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "TrackingRepository"

// MVP6 Tracking local — thuần Room, không có API (cùng tính chất
// FavoriteRepository). Đợt 1: status. Đợt 2: tiến độ tập + điểm cá nhân.
interface TrackingRepository {
    fun observeAll(): Flow<List<TrackedAnime>>
    fun observeTracking(malId: Int): Flow<TrackedAnime?>

    // Ngữ nghĩa toggle: chọn status mới = set, bấm lại status đang chọn = bỏ
    // (xem TrackingDao.toggleStatus).
    suspend fun toggleStatus(anime: Anime, status: WatchStatus)

    // episodesWatched <= 0 nghĩa là "bỏ tiến độ" (lưu null, không lưu 0) —
    // clamp giá trị âm ở đây, UI chịu trách nhiệm clamp cận trên (tổng số tập,
    // xem EpisodesSection).
    suspend fun updateEpisodesWatched(anime: Anime, episodesWatched: Int)

    // score = null nghĩa là "xoá điểm đã chấm".
    suspend fun updatePersonalScore(anime: Anime, score: Int?)
}

class TrackingRepositoryImpl @Inject constructor(
    private val dao: TrackingDao,
) : TrackingRepository {

    override fun observeAll(): Flow<List<TrackedAnime>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTracking(malId: Int): Flow<TrackedAnime?> =
        dao.observe(malId).map { it?.toDomain() }

    override suspend fun toggleStatus(anime: Anime, status: WatchStatus) {
        try {
            dao.toggleStatus(anime.toTrackingEntity(status, System.currentTimeMillis()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Local-only, không nghiêm trọng — chip trạng thái không đổi, user
            // bấm lại được (cùng cách FavoriteRepository xử lý).
            AppLog.w(TAG, "Không toggle được watch status cho malId=${anime.malId}", e)
        }
    }

    override suspend fun updateEpisodesWatched(anime: Anime, episodesWatched: Int) {
        try {
            dao.updateEpisodesWatched(
                anime.toTrackingSnapshot(System.currentTimeMillis()),
                episodesWatched.takeIf { it > 0 },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Không cập nhật được tiến độ tập cho malId=${anime.malId}", e)
        }
    }

    override suspend fun updatePersonalScore(anime: Anime, score: Int?) {
        try {
            // coerceIn(1, 10) chỉ áp dụng khi score != null — null luôn nghĩa
            // là "xoá điểm" (không bị coerce thành 1), xem interface doc ở trên.
            dao.updatePersonalScore(anime.toTrackingSnapshot(System.currentTimeMillis()), score?.coerceIn(1, 10))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Không cập nhật được điểm cá nhân cho malId=${anime.malId}", e)
        }
    }
}
