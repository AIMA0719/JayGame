package com.jay.jaygame

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.data.TimeGuard
import com.jay.jaygame.audio.SfxManager
import com.jay.jaygame.engine.BlueprintRegistry
import com.jay.jaygame.engine.RecipeSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JayGameApplication : Application(), ImageLoaderFactory {
    @Volatile
    lateinit var repository: GameRepository
        private set

    internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _initReady = MutableStateFlow(false)
    val initReady: StateFlow<Boolean> = _initReady

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        TimeGuard.onSessionStart()
        SfxManager.initAsync(this, appScope)
        appScope.launch {
            BlueprintRegistry.initialize(this@JayGameApplication)
            val recipeJob = launch { RecipeSystem.initialize(this@JayGameApplication) }
            val repoJob = launch(Dispatchers.IO) { repository = GameRepository(this@JayGameApplication) }
            recipeJob.join()
            repoJob.join()
            _initReady.value = true
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var appContext: android.content.Context
            private set
    }
}
