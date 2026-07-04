package com.lambao.animike.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    // Gộp Season Archive + Schedules vào 1 tab "Duyệt" (segmented control bên
    // trong BrowseScreen) để giữ đúng giới hạn 3-4 tab của animike-design SKILL.md.
    const val BROWSE = "browse"
    const val DETAIL = "detail/{malId}"
    const val DETAIL_ARG_MAL_ID = "malId"

    fun detail(malId: Int) = "detail/$malId"
}
