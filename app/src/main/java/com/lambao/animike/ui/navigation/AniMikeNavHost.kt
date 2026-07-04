package com.lambao.animike.ui.navigation

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.lambao.animike.ui.detail.DetailScreen
import com.lambao.animike.ui.episodes.EpisodesScreen
import com.lambao.animike.ui.favorites.FavoritesScreen
import com.lambao.animike.ui.home.HomeScreen
import com.lambao.animike.ui.reviews.ReviewsScreen
import com.lambao.animike.ui.search.SearchFilterScreen
import com.lambao.animike.ui.search.SearchScreen
import com.lambao.animike.ui.search.SearchViewModel

private data class BottomNavItem(val route: String, val label: String, val icon: String)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Trang chủ", "⌂"),
    BottomNavItem(Routes.SEARCH, "Tìm kiếm", "🔍"),
    BottomNavItem(Routes.BROWSE, "Duyệt", "📅"),
    // Đổi tên/icon theo kit Animax MVP3 UI-5 ("My List" dùng icon bookmark,
    // không phải trái tim) — nội dung vẫn là danh sách favorite như cũ.
    BottomNavItem(Routes.FAVORITES, "Danh sách", "🔖"),
)

@Composable
fun AniMikeNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            // Bottom nav chỉ hiện ở 4 tab gốc, ẩn khi push sang Detail.
            if (bottomNavItems.any { it.route == currentRoute }) {
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
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                    onNavigateToAnimeList = { source -> navController.navigate(Routes.animeList(source)) },
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
            ) {
                // malId được DetailViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                DetailScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                    onNavigateToEpisodes = { malId -> navController.navigate(Routes.episodes(malId)) },
                    onNavigateToCharacters = { malId -> navController.navigate(Routes.characters(malId)) },
                    onNavigateToReviews = { malId -> navController.navigate(Routes.reviews(malId)) },
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
            ) {
                // malId được ReviewsViewModel tự đọc qua SavedStateHandle, không cần truyền tay.
                ReviewsScreen(onBackClick = navController::popBackStack)
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
