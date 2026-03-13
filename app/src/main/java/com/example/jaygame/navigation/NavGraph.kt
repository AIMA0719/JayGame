package com.example.jaygame.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jaygame.data.GameRepository
import com.example.jaygame.ui.components.GameBottomNavBar
import com.example.jaygame.ui.components.NavTab
import com.example.jaygame.ui.screens.CollectionScreen
import com.example.jaygame.ui.screens.DeckScreen
import com.example.jaygame.ui.screens.HomeScreen
import com.example.jaygame.ui.screens.SettingsScreen
import com.example.jaygame.ui.screens.AchievementsScreen
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.screens.ShopScreen
import com.example.jaygame.ui.screens.UnitCollectionScreen
import com.example.jaygame.ui.theme.*
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    repository: GameRepository,
    onStartBattle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val selectedTab = when (currentRoute) {
        Routes.HOME -> NavTab.HOME
        Routes.DECK -> NavTab.DECK
        Routes.COLLECTION -> NavTab.COLLECTION
        Routes.SHOP -> NavTab.SHOP
        Routes.SETTINGS -> NavTab.SETTINGS
        else -> NavTab.HOME
    }

    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.DECK, Routes.COLLECTION, Routes.SHOP, Routes.SETTINGS,
    )

    Column(modifier = modifier
        .background(DeepDark)
        .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    onStartBattle = onStartBattle,
                )
            }
            composable(Routes.DECK) {
                DeckScreen(repository = repository)
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(repository = repository)
            }
            composable(Routes.SHOP) {
                ShopScreen(repository = repository)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    repository = repository,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.SETTINGS) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.UNIT_CODEX) {
                UnitCollectionScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.RESULT}?victory={victory}&wave={wave}&gold={gold}&trophy={trophy}&kills={kills}&merges={merges}",
                arguments = listOf(
                    navArgument("victory") { type = NavType.BoolType; defaultValue = true },
                    navArgument("wave") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("gold") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("trophy") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("kills") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("merges") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                val args = entry.arguments!!
                ResultScreen(
                    victory = args.getBoolean("victory"),
                    waveReached = args.getInt("wave"),
                    goldEarned = args.getInt("gold"),
                    trophyChange = args.getInt("trophy"),
                    killCount = args.getInt("kills"),
                    mergeCount = args.getInt("merges"),
                    onGoHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onRetry = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
        }

        if (showBottomBar) {
            GameBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    val route = when (tab) {
                        NavTab.DECK -> Routes.DECK
                        NavTab.HOME -> Routes.HOME
                        NavTab.COLLECTION -> Routes.COLLECTION
                        NavTab.SHOP -> Routes.SHOP
                        NavTab.SETTINGS -> Routes.SETTINGS
                    }
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
