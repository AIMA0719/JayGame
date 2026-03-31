package com.example.jaygame

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.TimeGuard
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.RecipeSystem

class JayGameApplication : Application(), ImageLoaderFactory {
    lateinit var repository: GameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        TimeGuard.onSessionStart()
        BlueprintRegistry.initialize(this)
        RecipeSystem.initialize(this)
        repository = GameRepository(this)
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
