package com.example.jaygame.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.jaygame.JayGameApplication
import com.example.jaygame.data.GameRepository

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(repository)
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository)
        modelClass.isAssignableFrom(ShopViewModel::class.java) -> ShopViewModel(repository)
        modelClass.isAssignableFrom(CollectionViewModel::class.java) -> CollectionViewModel(repository)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository)
        modelClass.isAssignableFrom(DeckViewModel::class.java) -> DeckViewModel(repository)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}

@Composable
fun gameViewModelFactory(): GameViewModelFactory {
    val app = LocalContext.current.applicationContext as JayGameApplication
    return remember { GameViewModelFactory(app.repository) }
}
