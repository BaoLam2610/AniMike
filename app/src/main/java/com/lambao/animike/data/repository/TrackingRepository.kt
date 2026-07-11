package com.lambao.animike.data.repository

import android.util.Log
import com.lambao.animike.data.local.dao.TrackingDao
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toTrackingEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.TrackedAnime
import com.lambao.animike.domain.model.WatchStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "TrackingRepository"

// MVP6 Tracking local — thuần Room, không có API (cùng tính chất
// FavoriteRepository). Đợt 1: status; đợt 2 sẽ thêm episodesWatched/personalScore.
interface TrackingRepository {
    fun observeAll(): Flow<List<TrackedAnime>>
    fun observeTracking(malId: Int): Flow<TrackedAnime?>

    // Ngữ nghĩa toggle: chọn status mới = set, bấm lại status đang chọn = bỏ
    // (xem TrackingDao.toggleStatus).
    suspend fun toggleStatus(anime: Anime, status: WatchStatus)
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
            Log.w(TAG, "Không toggle được watch status cho malId=${anime.malId}", e)
        }
    }
}
