package com.lambao.animike.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lambao.animike.ui.detail.DetailScreen
import com.lambao.animike.ui.home.HomeScreen

@Composable
fun AniMikeNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDetail = { malId -> navController.navigate(Routes.detail(malId)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.DETAIL_ARG_MAL_ID) { type = NavType.IntType }),
        ) { backStackEntry ->
            val malId = backStackEntry.arguments?.getInt(Routes.DETAIL_ARG_MAL_ID) ?: return@composable
            DetailScreen(malId = malId, onBackClick = navController::popBackStack)
        }
    }
}
