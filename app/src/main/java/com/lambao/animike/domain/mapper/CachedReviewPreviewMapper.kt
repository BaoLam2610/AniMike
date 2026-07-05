package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.ReviewReactions
import com.lambao.animike.domain.model.ReviewTag

// Delimiter ASCII hiếm gặp — cùng kỹ thuật CachedAnimeStatisticsMapper dùng
// cho scoreDistribution (1 object cố định hình dạng, không cần TypeConverter).
private const val FIELD_DELIMITER = ":::"

// Preview cache giờ lưu ĐỦ field như ReviewsScreen (theo yêu cầu user: đồng
// bộ hiển thị tab "Đánh giá" ở Detail với màn Đánh giá đầy đủ) — trước đây
// cố tình chỉ lưu 4 field tối thiểu, nay mở rộng để ReviewCard dùng chung
// không còn hiện thiếu avatar/date/tag/reactions ở Detail.
fun CachedReviewPreviewEntity.toDomain(): AnimeReview = AnimeReview(
    id = reviewId,
    username = username,
    userAvatarUrl = userAvatarUrl,
    score = score,
    reviewText = reviewText,
    date = date,
    tag = tag?.let { raw -> runCatching { ReviewTag.valueOf(raw) }.getOrNull() },
    reactions = decodeReactions(reactionsEncoded),
)

fun AnimeReview.toPreviewEntity(malId: Int, position: Int, fetchedAt: Long): CachedReviewPreviewEntity =
    CachedReviewPreviewEntity(
        malId = malId,
        reviewId = id,
        username = username,
        score = score,
        reviewText = reviewText,
        userAvatarUrl = userAvatarUrl,
        date = date,
        tag = tag?.name,
        reactionsEncoded = reactions?.let { encodeReactions(it) },
        position = position,
        fetchedAt = fetchedAt,
    )

private fun encodeReactions(reactions: ReviewReactions): String = listOf(
    reactions.overall,
    reactions.nice,
    reactions.loveIt,
    reactions.funny,
    reactions.confusing,
    reactions.informative,
    reactions.wellWritten,
    reactions.creative,
).joinToString(FIELD_DELIMITER)

private fun decodeReactions(encoded: String?): ReviewReactions? {
    if (encoded == null) return null
    val parts = encoded.split(FIELD_DELIMITER).mapNotNull { it.toIntOrNull() }
    // size != 8: dữ liệu hỏng (không đúng invariant do encodeReactions luôn
    // ghi đủ 8 số) — trả null thay vì crash hay dựng object nửa vời.
    if (parts.size != 8) return null
    return ReviewReactions(
        overall = parts[0],
        nice = parts[1],
        loveIt = parts[2],
        funny = parts[3],
        confusing = parts[4],
        informative = parts[5],
        wellWritten = parts[6],
        creative = parts[7],
    )
}
