package com.jay.jaygame

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.withContext

class JayGameApplication : Application(), ImageLoaderFactory {
    companion object {
        private const val TAG = "JayGameApplication"
        lateinit var appContext: android.content.Context
            private set
    }

    @Volatile
    lateinit var repository: GameRepository
        private set

    internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _initReady = MutableStateFlow(false)
    val initReady: StateFlow<Boolean> = _initReady
    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        TimeGuard.onSessionStart()
        SfxManager.initAsync(this, appScope)
        appScope.launch {
            _initReady.value = false
            _initError.value = null
            runCatching {
                check(BlueprintRegistry.initialize(this@JayGameApplication)) { "Blueprint initialization failed" }
                check(RecipeSystem.initialize(this@JayGameApplication)) { "Recipe initialization failed" }
                repository = withContext(Dispatchers.IO) { GameRepository(this@JayGameApplication) }
            }.onSuccess {
                _initReady.value = true
            }.onFailure { error ->
                Log.e(TAG, "App initialization failed", error)
                _initError.value = error.message ?: "Initialization failed"
            }
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
}
