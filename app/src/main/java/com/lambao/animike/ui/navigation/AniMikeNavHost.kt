package com.lambao.animike.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lambao.animike.ui.animelist.AnimeListScreen
import com.lambao.animike.ui.browse.BrowseScreen
import com.lambao.animike.ui.characters.CharactersScreen
import com.lambao.animike.ui.communityrecommendations.CommunityRecommendationsScreen
import com.lambao.animike.ui.detail.DetailScreen
import com.lambao.animike.ui.detail.DetailViewModel
import com.lambao.animike.ui.episodes.EpisodesScreen
import com.lambao.animike.ui.favorites.FavoritesScreen
import com.lambao.animike.ui.home.HomeScreen
import com.lambao.animike.ui.newepisodes.NewEpisodesScreen
import com.lambao.animike.ui.reviewdetail.ReviewDetailScreen
import com.lambao.animike.ui.reviews.ReviewsScreen
import com.lambao.animike.ui.reviews.ReviewsViewModel
import com.lambao.animike.ui.search.SearchFilterScreen
import com.lambao.animike.ui.search.SearchScreen
import com.lambao.animike.ui.search.SearchViewModel
import com.lambao.animike.ui.theme.Motion

private data class BottomNavItem(val route: String, val label: String, val icon: String)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Trang chủ", "⌂"),
    BottomNavItem(Routes.SEARCH, "Tìm kiếm", "🔍"),
    BottomNavItem(Routes.BROWSE, "Duyệt", "📅"),
    // Đổi tên/icon theo kit Animax MVP3 UI-5 ("My List" dùng icon bookmark,
    // không phải trái tim) — nội dung vẫn là danh sách favorite như cũ.
    BottomNavItem(Routes.FAVORITES, "Danh sách", "🔖"),
)

// Polish motion/transition giữa các màn hình (MVP3, mục cuối cùng). 2 loại:
// - Đổi tab bottom-nav (cả 2 đầu đều là 1 trong 4 route gốc) → crossfade nhẹ,
//   vì đây là các màn NGANG HÀNG (sibling), không phải drill-down.
// - Mọi điều hướng còn lại (vào Detail, "Xem tất cả", back...) → slide + fade,
//   cảm giác "đẩy vào/kéo ra" đúng ngữ nghĩa push/pop của back stack.
// Định nghĩa DÙNG CHUNG ở cấp NavHost thay vì lặp lại per-composable() — 1 route
// như SEARCH vừa là tab gốc (fade khi đổi từ Home) vừa là điểm push (slide khi
// mở SearchFilter), nên phải quyết định theo CẶP route thực tế đang chuyển
// tiếp (initialState/targetState) chứ không gán cứng theo từng composable().
//
// Duration/easing lấy từ ui/theme/Motion.kt (giá trị theo Material 3 motion
// spec, xem compose-expert/references/material3-motion.md — KHÔNG import
// trực tiếp androidx.compose.material3.tokens.MotionTokens được vì object đó
// internal với module material3, build lỗi "Cannot access... internal in file"
// nếu dùng từ module app). DurationMedium2 (300ms) cho slide/push khớp
// "dialog, bottom sheet, nav drawer"; DurationShort4 (200ms) cho crossfade
// tab khớp "chip selection". Enter LUÔN dùng easing Decelerate (vào nhanh,
// dừng êm), exit LUÔN dùng Accelerate (ra chậm rồi tăng tốc) — không bao giờ
// dùng chung 1 easing cho cả 2 chiều (quy tắc "Enter/exit rule" của skill).
private fun isBottomNavRoute(route: String?): Boolean = bottomNavItems.any { it.route == route }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    isBottomNavRoute(initialState.destination.route) && isBottomNavRoute(targetState.destination.route)

private val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val enterSpec = tween<Float>(
        durationMillis = Motion.DurationShort4,
        easing = Motion.EasingEmphasizedDecelerate,
    )
    if (isTabSwitch()) {
        fadeIn(animationSpec = enterSpec)
    } else {
        val slideSpec = tween<IntOffset>(
            durationMillis = Motion.DurationMedium2,
            easing = Motion.EasingEmphasizedDecelerate,
        )
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideSpec) + fadeIn(animationSpec = enterSpec)
    }
}

private val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val exitSpec = tween<Float>(
        durationMillis = Motion.DurationShort4,
        easing = Motion.EasingEmphasizedAccelerate,
    )
    if (isTabSwitch()) {
        fadeOut(animationSpec = exitSpec)
    } else {
        val slideSpec = tween<IntOffset>(
            durationMillis = Motion.DurationMedium2,
            easing = Motion.EasingEmphasizedAccelerate,
        )
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideSpec) + fadeOut(animationSpec = exitSpec)
    }
}

private val navPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val enterSpec = tween<Float>(
        durationMillis = Motion.DurationShort4,
        easing = Motion.EasingEmphasizedDecelerate,
    )
    if (isTabSwitch()) {
        fadeIn(animationSpec = enterSpec)
    } else {
        val slideSpec = tween<IntOffset>(
            durationMillis = Motion.DurationMedium2,
            easing = Motion.EasingEmphasizedDecelerate,
        )
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slideSpec) + fadeIn(animationSpec = enterSpec)
    }
}

private val navPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val exitSpec = tween<Float>(
        durationMillis = Motion.DurationShort4,
        easing = Motion.EasingEmphasizedAccelerate,
    )
    if (isTabSwitch()) {
        fadeOut(animationSpec = exitSpec)
    } else {
        val slideSpec = tween<IntOffset>(
            durationMillis = Motion.DurationMedium2,
            easing = Motion.EasingEmphasizedAccelerate,
        )
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slideSpec) + fadeOut(animationSpec = exitSpec)
    }
}

@Composable
fun AniMikeNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            // Bottom nav chỉ hiện ở 4 tab gốc, fade mượt khi push sang Detail
            // thay vì cắt cứng — cùng nhịp easing với transition của NavHost.
            AnimatedVisibility(
                visible = bottomNavItems.any { it.route == currentRoute },
                enter = fadeIn(
                    animationSpec = tween(
                        Motion.DurationShort4,
                        easing = Motion.EasingEmphasizedDecelerate,
                    ),
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        Motion.DurationShort4,
                        easing = Motion.EasingEmphasizedAccelerate,
                    ),
                ),
            ) {
                AniMikeBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition,
            popEnterTransition = navPopEnterTransition,
            popExitTransition = navPopExitTransition,
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                    onNavigateToAnimeList = { source -> navController.navigate(Routes.animeList(source)) },
                    onNavigateToNewEpisodes = { navController.navigate(Routes.NEW_EPISODES) },
                    onNavigateToCommunityRecommendations = {
                        navController.navigate(Routes.COMMUNITY_RECOMMENDATIONS)
                    },
                )
            }
            composable(
                route = Routes.ANIME_LIST,
                arguments = listOf(navArgument(Routes.ANIME_LIST_ARG_SOURCE) { type = NavType.StringType }),
            ) {
                // source được AnimeListViewModel tự đọc qua SavedStateHandle.
                AnimeListScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.NEW_EPISODES) {
                NewEpisodesScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.COMMUNITY_RECOMMENDATIONS) {
                CommunityRecommendationsScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.SEARCH) { backStackEntry ->
                SearchScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                    onNavigateToFilter = { navController.navigate(Routes.SEARCH_FILTER) },
                    viewModel = hiltViewModel(backStackEntry),
                )
            }
            composable(Routes.SEARCH_FILTER) {
                // Dùng chung SearchViewModel với Routes.SEARCH (scope theo backstack
                // entry của route cha) — filter sửa xong Apply thì ghi thẳng vào
                // đúng instance đang hiển thị kết quả, không cần truyền qua lại thủ công.
                val searchEntry = remember(it) { navController.getBackStackEntry(Routes.SEARCH) }
                SearchFilterScreen(
                    onBackClick = navController::popBackStack,
                    viewModel = hiltViewModel<SearchViewModel>(searchEntry),
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.DETAIL_ARG_MAL_ID) { type = NavType.IntType }),
            ) { backStackEntry ->
                // malId được DetailViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                // Truyền viewModel tường minh (thay vì default hiltViewModel()) để
                // DETAIL_REVIEW_DETAIL lấy lại ĐÚNG instance này qua getBackStackEntry.
                DetailScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                    onNavigateToEpisodes = { malId -> navController.navigate(Routes.episodes(malId)) },
                    onNavigateToCharacters = { malId -> navController.navigate(Routes.characters(malId)) },
                    onNavigateToReviews = { malId -> navController.navigate(Routes.reviews(malId)) },
                    onNavigateToReviewDetail = { navController.navigate(Routes.DETAIL_REVIEW_DETAIL) },
                    viewModel = hiltViewModel(backStackEntry),
                )
            }
            composable(
                route = Routes.EPISODES,
                arguments = listOf(navArgument(Routes.EPISODES_ARG_MAL_ID) { type = NavType.IntType }),
            ) {
                // malId được EpisodesViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                EpisodesScreen(onBackClick = navController::popBackStack)
            }
            composable(
                route = Routes.CHARACTERS,
                arguments = listOf(navArgument(Routes.CHARACTERS_ARG_MAL_ID) { type = NavType.IntType }),
            ) {
                // malId được CharactersViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                CharactersScreen(onBackClick = navController::popBackStack)
            }
            composable(
                route = Routes.REVIEWS,
                arguments = listOf(navArgument(Routes.REVIEWS_ARG_MAL_ID) { type = NavType.IntType }),
            ) { backStackEntry ->
                // malId được ReviewsViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                // Truyền viewModel tường minh (thay vì default hiltViewModel()) để
                // ReviewDetailScreen lấy lại ĐÚNG instance này qua getBackStackEntry.
                ReviewsScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToReviewDetail = { navController.navigate(Routes.REVIEW_DETAIL) },
                    viewModel = hiltViewModel(backStackEntry),
                )
            }
            composable(Routes.REVIEW_DETAIL) {
                // Dùng chung ReviewsViewModel với Routes.REVIEWS (scope theo backstack
                // entry của route cha) — review đang xem đã lưu sẵn trong
                // ReviewsState.selectedReview lúc bấm, không cần fetch lại/truyền arg.
                // ReviewDetailScreen không tự có ViewModel (dùng ở cả đây lẫn
                // DETAIL_REVIEW_DETAIL bên dưới) nên tự collect state ở đây rồi
                // truyền review xuống làm tham số thuần.
                val reviewsEntry = remember(it) { navController.getBackStackEntry(Routes.REVIEWS) }
                val reviewsState by hiltViewModel<ReviewsViewModel>(reviewsEntry).state.collectAsStateWithLifecycle()
                ReviewDetailScreen(
                    review = reviewsState.selectedReview,
                    onBackClick = navController::popBackStack,
                )
            }
            composable(Routes.DETAIL_REVIEW_DETAIL) {
                // Tương tự REVIEW_DETAIL nhưng dùng chung DetailViewModel (tab
                // "Đánh giá" ở Detail) — xem comment ở Routes.DETAIL_REVIEW_DETAIL.
                val detailEntry = remember(it) { navController.getBackStackEntry(Routes.DETAIL) }
                val detailState by hiltViewModel<DetailViewModel>(detailEntry).state.collectAsStateWithLifecycle()
                ReviewDetailScreen(
                    review = detailState.selectedReview,
                    onBackClick = navController::popBackStack,
                )
            }
        }
    }
}

@Composable
private fun AniMikeBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Text(text = item.icon) },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
