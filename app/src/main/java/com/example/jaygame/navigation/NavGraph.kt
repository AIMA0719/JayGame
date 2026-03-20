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
import com.example.jaygame.engine.PetManager
import com.example.jaygame.engine.RelicManager
import com.example.jaygame.ui.screens.CollectionScreen

import com.example.jaygame.ui.screens.HomeScreen
import com.example.jaygame.ui.screens.SettingsScreen
import com.example.jaygame.ui.screens.AchievementsScreen
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.screens.ShopScreen
import com.example.jaygame.ui.screens.DungeonScreen
import com.example.jaygame.ui.screens.ProfileScreen
import com.example.jaygame.ui.screens.UnitCollectionScreen
import com.example.jaygame.ui.theme.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.widget.Toast

@Composable
fun NavGraph(
    repository: GameRepository,
    onStartBattle: () -> Unit,
    onStartDungeonBattle: (dungeonId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    // ── Hidden cheat: tap shop tab 10 times within 10 seconds ──
    val context = androidx.compose.ui.platform.LocalContext.current
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
                HomeScreen(
                    repository = repository,
                    onStartBattle = onStartBattle,
                    onNavigateToDungeon = {
                        navController.navigate(Routes.DUNGEON) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.COLLECTION) {
                val gameData by repository.gameData.collectAsState()
                val petMgr = remember { PetManager(gameData) }
                CollectionScreen(
                    repository = repository,
                    onRelicUpgrade = { relicId ->
                        val mgr = RelicManager(repository.gameData.value)
                        val updated = mgr.upgradeRelic(relicId)
                        if (updated != null) repository.save(updated)
                    },
                    onRelicEquip = { relicId ->
                        val mgr = RelicManager(repository.gameData.value)
                        val updated = mgr.equipRelic(relicId)
                        if (updated != null) repository.save(updated)
                    },
                    onRelicUnequip = { relicId ->
                        val mgr = RelicManager(repository.gameData.value)
                        repository.save(mgr.unequipRelic(relicId))
                    },
                    onPetPull = {
                        petMgr.syncData(repository.gameData.value)
                        val updated = petMgr.pullPet()
                        if (updated != null) repository.save(updated)
                    },
                    onPetPull10 = {
                        petMgr.syncData(repository.gameData.value)
                        val updated = petMgr.pullPet10()
                        if (updated != null) repository.save(updated)
                    },
                    onPetUpgrade = { petId ->
                        petMgr.syncData(repository.gameData.value)
                        val updated = petMgr.upgradePet(petId)
                        if (updated != null) repository.save(updated)
                    },
                    onPetEquip = { petId ->
                        petMgr.syncData(repository.gameData.value)
                        val updated = petMgr.equipPet(petId)
                        if (updated != null) repository.save(updated)
                    },
                    onPetUnequip = { petId ->
                        petMgr.syncData(repository.gameData.value)
                        repository.save(petMgr.unequipPet(petId))
                    },
                )
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
                        // Keep only taps within the last 10 seconds
                        shopTapTimes.removeAll { now - it > 10_000L }
                        if (shopTapTimes.size >= 10) {
                            cheatActivated = true
                            shopTapTimes.clear()
                            val current = repository.gameData.value
                            repository.save(
                                current.copy(
                                    diamonds = 9_999_999,
                                    gold = 9_999_999,
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
