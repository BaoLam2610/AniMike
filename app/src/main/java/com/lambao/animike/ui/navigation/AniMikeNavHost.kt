package com.lambao.animike.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lambao.animike.ui.detail.DetailScreen
import com.lambao.animike.ui.home.HomeScreen
import com.lambao.animike.ui.search.SearchScreen

@Composable
fun AniMikeNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBackClick = navController::popBackStack,
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
