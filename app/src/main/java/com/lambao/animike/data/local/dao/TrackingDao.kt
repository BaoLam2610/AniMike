package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.TrackingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TrackingEntity>>

    @Query("SELECT * FROM tracking WHERE malId = :malId")
    fun observe(malId: Int): Flow<TrackingEntity?>

    @Query("SELECT * FROM tracking WHERE malId = :malId")
    suspend fun getOnce(malId: Int): TrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrackingEntity)

    @Query("DELETE FROM tracking WHERE malId = :malId")
    suspend fun delete(malId: Int)

    // Gộp đọc + ghi vào 1 transaction — cùng lý do TOCTOU với FavoriteDao.toggle.
    // Ngữ nghĩa: chọn trạng thái mới = set; bấm LẠI đúng trạng thái đang chọn =
    // bỏ trạng thái (toggle-off) — xoá hẳn row nếu không còn dữ liệu tracking
    // nào khác (episodesWatched/personalScore của đợt 2), ngược lại chỉ null
    // hoá cột status để giữ tiến độ/điểm.
    @Transaction
    suspend fun toggleStatus(candidate: TrackingEntity) {
        val current = getOnce(candidate.malId)
        when {
            current == null -> upsert(candidate)

            current.status == candidate.status -> {
                if (current.episodesWatched == null && current.personalScore == null) {
                    delete(candidate.malId)
                } else {
                    upsert(current.copy(status = null, updatedAt = candidate.updatedAt))
                }
            }

            // Giữ episodesWatched/personalScore hiện có, cập nhật snapshot
            // hiển thị (title/ảnh có thể mới hơn) + status mới.
            else -> upsert(
                current.copy(
                    title = candidate.title,
                    imageUrl = candidate.imageUrl,
                    score = candidate.score,
                    year = candidate.year,
                    status = candidate.status,
                    updatedAt = candidate.updatedAt,
                ),
            )
        }
    }
}
