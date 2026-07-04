package com.lambao.animike.domain.model

/**
 * Nguồn dữ liệu cho màn danh sách anime dạng "Xem tất cả" từ Home (Paging 3).
 * Truyền qua route bằng `name` (Routes.animeList) — thêm nguồn mới chỉ cần
 * thêm entry + nhánh when trong AnimeListPagingSource/AnimeListScreen.
 */
enum class AnimeListSource {
    TOP,
    UPCOMING,
}
