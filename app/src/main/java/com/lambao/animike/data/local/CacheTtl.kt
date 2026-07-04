package com.lambao.animike.data.local

/** TTL cache theo loại dữ liệu (.claude/skills/jikan-api SKILL.md mục Caching). */
object CacheTtl {
    const val LIST_MS = 24 * 60 * 60 * 1000L
    const val DETAIL_MS = 24 * 60 * 60 * 1000L
    const val GENRE_MS = 7 * 24 * 60 * 60 * 1000L
    const val SEASON_LIST_MS = 7 * 24 * 60 * 60 * 1000L

    // TTL ngắn cho cache tạm trong bộ nhớ (không phải Room) ở
    // AnimeDetailRepository — chỉ để tránh gọi trùng API khi user bấm "Xem
    // tất cả" ngay sau khi Detail vừa tải xong (không nhằm cache lâu dài,
    // xem docs/ROADMAP.md mục 3b cho kế hoạch cache Room đầy đủ sau này).
    const val MEMORY_DEDUP_MS = 5 * 60 * 1000L
}

fun isExpired(fetchedAt: Long, ttlMs: Long, now: Long = System.currentTimeMillis()): Boolean {
    val age = now - fetchedAt
    // age < 0 nghĩa là fetchedAt ở tương lai (lệch giờ máy) — coi là hết hạn
    // luôn thay vì "còn mới vĩnh viễn" cho tới khi đồng hồ thật đuổi kịp.
    return age < 0 || age > ttlMs
}
