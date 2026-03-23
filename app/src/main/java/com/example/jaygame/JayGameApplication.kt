package com.example.jaygame

import android.app.Application
import com.example.jaygame.data.GameRepository
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.RecipeSystem

class JayGameApplication : Application() {
    lateinit var repository: GameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        BlueprintRegistry.initialize(this)
        RecipeSystem.initialize(this)
        repository = GameRepository(this)
    }
}
