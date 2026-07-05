package com.lambao.animike.data.local

/** TTL cache theo loại dữ liệu (.claude/skills/jikan-api SKILL.md mục Caching). */
object CacheTtl {
    const val LIST_MS = 24 * 60 * 60 * 1000L
    const val DETAIL_MS = 24 * 60 * 60 * 1000L
    const val GENRE_MS = 7 * 24 * 60 * 60 * 1000L
    const val SEASON_LIST_MS = 7 * 24 * 60 * 60 * 1000L

    // TTL ngắn cho cache tạm trong bộ nhớ (không phải Room) ở
    // AnimeDetailRepository, dùng riêng cho Characters — chỉ để tránh gọi
    // trùng API khi user bấm "Xem tất cả" ngay sau khi Detail vừa tải xong,
    // không nhằm cache lâu dài như 3 hằng số Room bên dưới (docs/ROADMAP.md
    // mục 3b).
    const val MEMORY_DEDUP_MS = 5 * 60 * 1000L

    // Cache Room cho dữ liệu phụ ở Detail (docs/ROADMAP.md mục 3b) — KHÔNG áp
    // dụng cho "Các tập" (luôn gọi lại /videos/episodes mỗi lần vào Detail vì
    // tập có thể ra bất cứ lúc nào, cache dễ làm user tưởng đã xem hết trong
    // khi có tập mới). Recommendations/Pictures gần như tĩnh nên TTL dài như
    // genres; Reviews user đăng liên tục nên TTL ngắn hơn, giống LIST_MS.
    const val RECOMMENDATIONS_MS = 7 * 24 * 60 * 60 * 1000L
    const val PICTURES_MS = 7 * 24 * 60 * 60 * 1000L
    const val REVIEWS_PREVIEW_MS = 24 * 60 * 60 * 1000L
}

fun isExpired(fetchedAt: Long, ttlMs: Long, now: Long = System.currentTimeMillis()): Boolean {
    val age = now - fetchedAt
    // age < 0 nghĩa là fetchedAt ở tương lai (lệch giờ máy) — coi là hết hạn
    // luôn thay vì "còn mới vĩnh viễn" cho tới khi đồng hồ thật đuổi kịp.
    return age < 0 || age > ttlMs
}
