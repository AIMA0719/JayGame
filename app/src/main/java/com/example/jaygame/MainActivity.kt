package com.example.jaygame

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.jaygame.audio.BgmManager
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.STAGES
import com.example.jaygame.engine.BattleEngine
import com.example.jaygame.ui.battle.BattleScreen
import com.example.jaygame.ui.theme.JayGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private lateinit var repository: GameRepository
    private var engine: BattleEngine? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        repository = GameRepository(this)

        BattleBridge.reset()

        // Create and start Kotlin battle engine
        val stageId = BattleBridge.stageId.value
        val difficulty = BattleBridge.difficulty.value
        val stage = STAGES.getOrNull(stageId) ?: STAGES[0]
        val data = repository.gameData.value
        engine = BattleEngine(
            stageId = stageId,
            difficulty = difficulty,
            maxWaves = stage.maxWaves,
            deck = data.deck.toIntArray(),
        ).also {
            BattleBridge.engine = it
            it.start(engineScope)
        }

        // Play battle BGM
        if (data.musicEnabled) {
            BgmManager.play(this, "audio/battle_bgm.mp3")
        }

        setContent {
            JayGameTheme {
                val result by BattleBridge.result.collectAsState()
                BattleScreen(
                    result = result,
                    onGoHome = {
                        BattleBridge.clearResult()
                        finish()
                    },
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BgmManager.stop()
    }

    override fun onDestroy() {
        engine?.stop()
        BattleBridge.engine = null
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
