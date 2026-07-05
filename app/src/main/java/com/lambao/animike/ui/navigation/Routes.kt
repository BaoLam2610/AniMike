package com.lambao.animike.ui.navigation

import com.lambao.animike.domain.model.AnimeListSource

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    // Dùng chung SearchViewModel với SEARCH qua hiltViewModel(parentBackStackEntry)
    // — filter là draft cục bộ trong màn này, chỉ commit vào ViewModel khi Apply.
    const val SEARCH_FILTER = "search/filter"
    const val FAVORITES = "favorites"
    // Gộp Season Archive + Schedules vào 1 tab "Duyệt" (segmented control bên
    // trong BrowseScreen) để giữ đúng giới hạn 3-4 tab của animike-design SKILL.md.
    const val BROWSE = "browse"
    const val DETAIL = "detail/{malId}"
    const val DETAIL_ARG_MAL_ID = "malId"
    const val EPISODES = "episodes/{malId}"
    const val EPISODES_ARG_MAL_ID = "malId"
    const val CHARACTERS = "characters/{malId}"
    const val CHARACTERS_ARG_MAL_ID = "malId"
    const val REVIEWS = "reviews/{malId}"
    const val REVIEWS_ARG_MAL_ID = "malId"
    // Dùng chung ReviewsViewModel với REVIEWS (giống SEARCH_FILTER/SearchViewModel)
    // — review đang xem lưu trong ReviewsState.selectedReview, không cần arg.
    const val REVIEW_DETAIL = "reviews/detail"
    // Tương tự REVIEW_DETAIL nhưng cho tab "Đánh giá" ở Detail — dùng chung
    // DetailViewModel (DetailState.selectedReview), route riêng vì
    // getBackStackEntry(Routes.REVIEWS) sẽ crash nếu gọi khi KHÔNG đứng sau
    // Routes.REVIEWS trên backstack (trường hợp mở từ Detail).
    const val DETAIL_REVIEW_DETAIL = "detail/review-detail"
    // "Xem tất cả" từ Home (Top Hits / Sắp chiếu) — source là AnimeListSource.name
    const val ANIME_LIST = "animeList/{source}"
    const val ANIME_LIST_ARG_SOURCE = "source"
    // "Xem tất cả" của "Tập mới phát hành" (MVP4) — không có arg, 1 feed toàn cục.
    const val NEW_EPISODES = "newEpisodes"
    // "Xem tất cả" của "Đề xuất cộng đồng" (MVP4) — không có arg, Paging 3 riêng.
    const val COMMUNITY_RECOMMENDATIONS = "communityRecommendations"

    fun detail(malId: Int) = "detail/$malId"
    fun episodes(malId: Int) = "episodes/$malId"
    fun characters(malId: Int) = "characters/$malId"
    fun reviews(malId: Int) = "reviews/$malId"
    // Nhận enum thay vì String thô — compiler bảo đảm route luôn round-trip
    // được qua AnimeListSource.valueOf ở AnimeListViewModel.
    fun animeList(source: AnimeListSource) = "animeList/${source.name}"
}
