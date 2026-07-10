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
    // MVP5 Character Detail — mở từ CharactersScreen (grid) hoặc tab preview
    // ở Detail. Đặt tên arg "characterId" (khác "malId") vì đây là aggregate
    // KHÁC (khoá theo id nhân vật, không phải id anime).
    const val CHARACTER_DETAIL = "character/{characterId}"
    const val CHARACTER_DETAIL_ARG_CHARACTER_ID = "characterId"
    // MVP5 People/Seiyuu Detail — mở từ "Lồng tiếng bởi" ở Character Detail
    // hoặc "Ê-kíp sản xuất" ở Detail. Arg tên "personId" (khác "malId"/
    // "characterId") — aggregate thứ 3, khoá theo id người.
    const val PERSON_DETAIL = "person/{personId}"
    const val PERSON_DETAIL_ARG_PERSON_ID = "personId"
    // MVP5 Studio Detail — mở từ chip studio ở Detail. Arg "studioId"
    // (= producer mal_id) — aggregate thứ 4.
    const val STUDIO_DETAIL = "studio/{studioId}"
    const val STUDIO_DETAIL_ARG_STUDIO_ID = "studioId"
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
    // "Xem tất cả" của "Nhân vật nổi bật" (MVP5) — không có arg, Paging 3 riêng.
    const val TOP_CHARACTERS = "topCharacters"

    fun detail(malId: Int) = "detail/$malId"
    fun episodes(malId: Int) = "episodes/$malId"
    fun characters(malId: Int) = "characters/$malId"
    fun characterDetail(characterId: Int) = "character/$characterId"
    fun personDetail(personId: Int) = "person/$personId"
    fun studioDetail(studioId: Int) = "studio/$studioId"
    fun reviews(malId: Int) = "reviews/$malId"
    // Nhận enum thay vì String thô — compiler bảo đảm route luôn round-trip
    // được qua AnimeListSource.valueOf ở AnimeListViewModel.
    fun animeList(source: AnimeListSource) = "animeList/${source.name}"
}
