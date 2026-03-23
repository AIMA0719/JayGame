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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jaygame.JayGameApplication
import com.example.jaygame.ui.components.GameBottomNavBar
import com.example.jaygame.ui.components.NavTab
import com.example.jaygame.ui.screens.CollectionScreen
import com.example.jaygame.ui.screens.HomeScreen
import com.example.jaygame.ui.screens.SettingsScreen
import com.example.jaygame.ui.screens.AchievementsScreen
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.screens.ShopScreen
import com.example.jaygame.ui.screens.DungeonScreen
import com.example.jaygame.data.STAGES
import com.example.jaygame.ui.screens.ProfileScreen
import com.example.jaygame.ui.screens.DeckScreen
import com.example.jaygame.ui.screens.UnitCollectionScreen
import com.example.jaygame.ui.theme.*
import com.example.jaygame.ui.viewmodel.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.widget.Toast

@Composable
fun NavGraph(
    factory: ViewModelProvider.Factory,
    onStartBattle: () -> Unit,
    onStartDungeonBattle: (dungeonId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    // ── Hidden cheat: tap shop tab 10 times within 10 seconds ──
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = (context.applicationContext as JayGameApplication).repository
    val shopTapTimes = remember { mutableListOf<Long>() }
    var cheatActivated by remember { mutableStateOf(false) }

    val selectedTab = when (currentRoute) {
        Routes.HOME -> NavTab.HOME
        Routes.COLLECTION -> NavTab.COLLECTION
        Routes.SHOP -> NavTab.SHOP
        Routes.SETTINGS -> NavTab.SETTINGS
        else -> NavTab.HOME
    }

    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.COLLECTION, Routes.SHOP, Routes.SETTINGS,
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
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    onStartBattle = onStartBattle,
                    onNavigateToDungeon = {
                        navController.navigate(Routes.DUNGEON) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToDeck = {
                        navController.navigate(Routes.DECK) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.COLLECTION) {
                val vm: CollectionViewModel = viewModel(factory = factory)
                CollectionScreen(viewModel = vm)
            }
            composable(Routes.SHOP) {
                val vm: ShopViewModel = viewModel(factory = factory)
                ShopScreen(viewModel = vm)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
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
                    repository = repository,
                )
            }
            composable(Routes.DUNGEON) {
                DungeonScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onStartDungeonBattle = onStartDungeonBattle,
                )
            }
            composable(Routes.DECK) {
                val vm: DeckViewModel = viewModel(factory = factory)
                DeckScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    repository = repository,
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
                val args = entry.arguments ?: return@composable
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
                    // ── Hidden cheat detection ──
                    if (tab == NavTab.SHOP && !cheatActivated) {
                        val now = System.currentTimeMillis()
                        shopTapTimes.add(now)
                        shopTapTimes.removeAll { now - it > 10_000L }
                        if (shopTapTimes.size >= 10) {
                            cheatActivated = true
                            shopTapTimes.clear()
                            val current = repository.gameData.value
                            val allUnlocked = current.units.mapValues { (_, u) -> u.copy(owned = true, cards = 999) }
                            repository.save(
                                current.copy(
                                    gold = 9_999_999,
                                    diamonds = 9_999_999,
                                    stamina = 9_999,
                                    maxStamina = 9_999,
                                    units = allUnlocked,
                                    unlockedStages = (0 until STAGES.size).toList(),
                                ),
                            )
                            Toast.makeText(context, "★ DEV MODE ★", Toast.LENGTH_SHORT).show()
                        }
                    }

                    val route = when (tab) {
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
