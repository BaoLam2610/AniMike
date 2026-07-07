package com.lambao.animike.data.local

/** TTL cache theo loại dữ liệu (.claude/skills/jikan-api SKILL.md mục Caching). */
object CacheTtl {
    const val LIST_MS = 24 * 60 * 60 * 1000L
    const val DETAIL_MS = 24 * 60 * 60 * 1000L
    const val GENRE_MS = 7 * 24 * 60 * 60 * 1000L
    const val SEASON_LIST_MS = 7 * 24 * 60 * 60 * 1000L

    // Cache Room cho dữ liệu phụ ở Detail (docs/ROADMAP.md mục 3b) — KHÔNG áp
    // dụng cho "Các tập" (luôn gọi lại /videos/episodes mỗi lần vào Detail vì
    // tập có thể ra bất cứ lúc nào, cache dễ làm user tưởng đã xem hết trong
    // khi có tập mới). Recommendations/Pictures/Characters gần như tĩnh nên
    // TTL dài như genres; Reviews user đăng liên tục nên TTL ngắn hơn, giống
    // LIST_MS.
    const val RECOMMENDATIONS_MS = 7 * 24 * 60 * 60 * 1000L
    const val PICTURES_MS = 7 * 24 * 60 * 60 * 1000L
    const val CHARACTERS_MS = 7 * 24 * 60 * 60 * 1000L
    const val REVIEWS_PREVIEW_MS = 24 * 60 * 60 * 1000L

    // MVP4 "Đề xuất cộng đồng" (Home preview, /recommendations/anime) — user
    // MAL đăng liên tục như Reviews nên TTL ngắn giống REVIEWS_PREVIEW_MS,
    // không dài như Recommendations/Pictures/Characters (vốn gần như tĩnh).
    const val COMMUNITY_RECOMMENDATIONS_MS = 24 * 60 * 60 * 1000L

    // MVP4 "Biểu đồ phân bố điểm" + "Nhạc OP/ED" (Detail, /statistics + /themes)
    // — số liệu thống kê nhích dần mỗi ngày nhưng không cần tươi theo giờ,
    // nhạc OP/ED gần như KHÔNG BAO GIỜ đổi sau khi anime phát sóng xong — cả
    // 2 dùng TTL dài như Recommendations/Pictures/Characters.
    const val STATISTICS_MS = 7 * 24 * 60 * 60 * 1000L
    const val THEMES_MS = 7 * 24 * 60 * 60 * 1000L

    // MVP4 nút "Xem trên..." + tab "Video" (Detail, /streaming + /videos) —
    // nền tảng phát hành và danh sách PV/MV gần như tĩnh sau khi anime lên
    // sóng, TTL dài như nhóm trên.
    const val STREAMING_MS = 7 * 24 * 60 * 60 * 1000L
    const val VIDEOS_MS = 7 * 24 * 60 * 60 * 1000L

    // MVP5 Character Detail (/characters/{id}/full) — tiểu sử/danh sách xuất
    // hiện/seiyuu của 1 nhân vật gần như tĩnh, TTL dài như Characters/Recommendations/Pictures.
    const val CHARACTER_DETAIL_MS = 7 * 24 * 60 * 60 * 1000L

    // MVP5 People/Seiyuu Detail (/people/{id}/full) + "Ê-kíp sản xuất"
    // (/anime/{id}/staff) — tiểu sử/credit của 1 người và ê-kíp 1 anime gần
    // như tĩnh, TTL dài như CHARACTER_DETAIL_MS.
    const val PERSON_DETAIL_MS = 7 * 24 * 60 * 60 * 1000L
    const val STAFF_MS = 7 * 24 * 60 * 60 * 1000L
}

fun isExpired(fetchedAt: Long, ttlMs: Long, now: Long = System.currentTimeMillis()): Boolean {
    val age = now - fetchedAt
    // age < 0 nghĩa là fetchedAt ở tương lai (lệch giờ máy) — coi là hết hạn
    // luôn thay vì "còn mới vĩnh viễn" cho tới khi đồng hồ thật đuổi kịp.
    return age < 0 || age > ttlMs
}
