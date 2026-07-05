package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.ReviewDto
import com.lambao.animike.data.remote.dto.ReviewReactionsDto
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.ReviewReactions
import com.lambao.animike.domain.model.ReviewTag
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

fun ReviewDto.toDomain(): AnimeReview = AnimeReview(
    id = malId,
    username = user?.username ?: "Ẩn danh",
    userAvatarUrl = user?.images?.jpg?.largeImageUrl ?: user?.images?.jpg?.imageUrl,
    score = score,
    reviewText = review ?: "",
    date = formatReviewDate(date),
    tag = parseReviewTag(tags),
    reactions = reactions?.toDomain(),
)

private fun ReviewReactionsDto.toDomain(): ReviewReactions = ReviewReactions(
    overall = overall ?: 0,
    nice = nice ?: 0,
    loveIt = loveIt ?: 0,
    funny = funny ?: 0,
    confusing = confusing ?: 0,
    informative = informative ?: 0,
    wellWritten = wellWritten ?: 0,
    creative = creative ?: 0,
)

// tags LUÔN đúng 1 phần tử theo verify qua curl (3 giá trị khả dĩ:
// Recommended/Mixed Feelings/Not Recommended — KHÁC giả định ban đầu chỉ có
// 2 loại) — lấy phần tử đầu, null nếu rỗng hoặc gặp giá trị không khớp.
private fun parseReviewTag(tags: List<String>): ReviewTag? = when (tags.firstOrNull()) {
    "Recommended" -> ReviewTag.RECOMMENDED
    "Mixed Feelings" -> ReviewTag.MIXED_FEELINGS
    "Not Recommended" -> ReviewTag.NOT_RECOMMENDED
    else -> null
}

// SimpleDateFormat không thread-safe — tạo instance mới mỗi lần gọi thay vì
// field dùng chung (rẻ, vì mapper chỉ chạy trên 1 trang ~20 review/lần).
// minSdk 24 hỗ trợ pattern "XXX" (ISO offset) từ API 24 — không cần
// java.time (yêu cầu core library desugaring, project chưa bật).
private fun formatReviewDate(raw: String?): String? {
    if (raw == null) return null
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(raw)
        if (parsed != null) SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(parsed) else raw
    } catch (e: ParseException) {
        raw
    }
}
