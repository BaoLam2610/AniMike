package com.lambao.animike.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{malId}"
    const val DETAIL_ARG_MAL_ID = "malId"

    fun detail(malId: Int) = "detail/$malId"
}
