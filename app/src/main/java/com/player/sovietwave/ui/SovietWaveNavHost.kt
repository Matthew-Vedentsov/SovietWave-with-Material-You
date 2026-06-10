package com.player.sovietwave.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val PLAYER = "player"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
}

@Composable
fun SovietWaveNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PLAYER,
    ) {
        composable(Routes.PLAYER) {
            RadioPlayerScreen(
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
            )
        }
        composable(Routes.HISTORY) {
            BroadcastHistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
