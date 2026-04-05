package com.jay.jaygame.navigation

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
import com.jay.jaygame.JayGameApplication
import com.jay.jaygame.ui.components.GameBottomNavBar
import com.jay.jaygame.ui.components.NavTab
import com.jay.jaygame.ui.screens.CollectionScreen
import com.jay.jaygame.ui.screens.HomeScreen
import com.jay.jaygame.ui.screens.SettingsScreen
import com.jay.jaygame.ui.screens.AchievementsScreen
import com.jay.jaygame.ui.screens.ShopScreen
import com.jay.jaygame.ui.screens.DungeonScreen
import com.jay.jaygame.data.STAGES
import com.jay.jaygame.data.UnitProgress
import com.jay.jaygame.engine.BlueprintRegistry
import com.jay.jaygame.ui.theme.*
import com.jay.jaygame.ui.viewmodel.*
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jay.jaygame.audio.BgmManager

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

    // 화면별 BGM 전환 — 라이프사이클 resume 시에도 재평가
    val musicEnabled = (context.applicationContext as JayGameApplication).repository.gameData.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var bgmRefreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) bgmRefreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(currentRoute, musicEnabled.value.musicEnabled, bgmRefreshKey) {
        if (!musicEnabled.value.musicEnabled) return@LaunchedEffect
        val bgmAsset = when (currentRoute) {
            Routes.COLLECTION -> "audio/collection_bgm.mp3"
            Routes.SHOP -> "audio/shop_bgm.mp3"
            Routes.DUNGEON -> "audio/dungeon_bgm.mp3"
            else -> "audio/home_bgm.mp3"
        }
        BgmManager.play(context, bgmAsset)
    }

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
            composable(Routes.DUNGEON) {
                DungeonScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onStartDungeonBattle = onStartDungeonBattle,
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
                            // 모든 블루프린트를 도감에 등록 (기존 맵에 없는 유닛 포함)
                            val allUnits = current.units.toMutableMap()
                            if (BlueprintRegistry.isReady) {
                                for (bp in BlueprintRegistry.instance.all()) {
                                    val existing = allUnits[bp.id]
                                    allUnits[bp.id] = (existing ?: UnitProgress()).copy(owned = true, cards = 999)
                                }
                            }
                            repository.save(
                                current.copy(
                                    gold = 9_999_999,
                                    diamonds = 9_999_999,
                                    stamina = 9_999,
                                    maxStamina = 9_999,
                                    units = allUnits,
                                    unlockedStages = (0 until STAGES.size).toList(),
                                    playerLevel = 999,
                                    trophies = 9_999,
                                    relics = current.relics.map { it.copy(owned = true, grade = 4, level = 10) },
                                    equippedRelics = current.relics.indices.toList(),
                                    pets = current.pets.map { it.copy(owned = true, level = 10, cards = 999) },
                                    equippedPets = current.pets.indices.take(current.equippedPetSlotCount).toList(),
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
