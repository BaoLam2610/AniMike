package com.lambao.animike.data.local.entity

import androidx.room.Entity

// userAvatarUrl/date/tag/reactionsEncoded: bổ sung để tab "Đánh giá" ở Detail
// đồng bộ hiển thị với ReviewsScreen (theo yêu cầu user) — trước đây cache
// preview cố tình chỉ lưu 4 field tối thiểu, nay mở rộng đủ để ReviewCard
// dùng chung không còn hiện thiếu dữ liệu ở Detail. reactionsEncoded: 8 số
// nguyên nối bằng delimiter ASCII (xem CachedReviewPreviewMapper.kt) — cùng
// kỹ thuật CachedAnimeStatisticsMapper dùng cho scoreDistribution, tránh
// thêm TypeConverter chỉ vì 1 object cố định hình dạng.
/** Cache cho preview "Đánh giá" (page 1, top 5) ở Detail — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_review_preview", primaryKeys = ["malId", "reviewId"])
data class CachedReviewPreviewEntity(
    val malId: Int,
    val reviewId: Int,
    val username: String,
    val score: Int?,
    val reviewText: String,
    val userAvatarUrl: String?,
    val date: String?,
    val tag: String?,
    val reactionsEncoded: String?,
    val position: Int,
    val fetchedAt: Long,
)
