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
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lambao.animike.ui.detail.DetailScreen
import com.lambao.animike.ui.favorites.FavoritesScreen
import com.lambao.animike.ui.home.HomeScreen
import com.lambao.animike.ui.search.SearchScreen
import com.lambao.animike.ui.seasonarchive.SeasonArchiveScreen

private data class BottomNavItem(val route: String, val label: String, val icon: String)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Trang chủ", "⌂"),
    BottomNavItem(Routes.SEARCH, "Tìm kiếm", "🔍"),
    BottomNavItem(Routes.SEASON_ARCHIVE, "Mùa", "📅"),
    BottomNavItem(Routes.FAVORITES, "Yêu thích", "♥"),
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
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                )
            }
            composable(Routes.SEASON_ARCHIVE) {
                SeasonArchiveScreen(
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
