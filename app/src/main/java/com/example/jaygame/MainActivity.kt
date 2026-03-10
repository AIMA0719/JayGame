package com.example.jaygame

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.battle.BattleBottomHud
import com.example.jaygame.ui.battle.BattleFieldGrid
import com.example.jaygame.ui.battle.BattleTopHud
import com.example.jaygame.ui.battle.MergeEffectOverlay
import com.example.jaygame.ui.battle.UnitDetailPopup
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.theme.JayGameTheme
import com.google.androidgamesdk.GameActivity

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("jaygame")
        }
    }

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 새 배틀 시작 시 이전 결과 초기화
        BattleBridge.reset()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < 2000) {
                    finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@MainActivity, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Add Compose overlays on top of C++ SurfaceView
        addBattleOverlay()
        addGridOverlay()
        addResultOverlay()
    }

    private fun addBattleOverlay() {
        // Top HUD overlay
        val topView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                JayGameTheme {
                    BattleTopHud()
                }
            }
        }
        addContentView(
            topView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            ),
        )

        // Bottom HUD overlay
        val bottomView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                JayGameTheme {
                    BattleBottomHud()
                }
            }
        }
        addContentView(
            bottomView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )
    }

    /**
     * Grid + popup + merge effect overlays.
     * Layered on top of the battle HUD, below the result overlay.
     */
    private fun addGridOverlay() {
        val gridView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                JayGameTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Grid in bottom-center area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 40.dp),
                        ) {
                            BattleFieldGrid()
                        }

                        // Unit detail popup (full screen overlay)
                        UnitDetailPopup()

                        // Merge effect overlay
                        MergeEffectOverlay()
                    }
                }
            }
        }
        addContentView(
            gridView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    /**
     * 배틀 결과 Compose 오버레이.
     * C++에서 BattleBridge.onBattleEnd()가 호출되면 전체 화면 ResultScreen 표시.
     * "홈으로" 버튼 → finish() → ComposeActivity로 복귀.
     */
    private fun addResultOverlay() {
        val resultView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                JayGameTheme {
                    val result by BattleBridge.result.collectAsState()
                    result?.let { data ->
                        ResultScreen(
                            victory = data.victory,
                            waveReached = data.waveReached,
                            goldEarned = data.goldEarned,
                            trophyChange = data.trophyChange,
                            killCount = data.killCount,
                            mergeCount = data.mergeCount,
                            onGoHome = {
                                BattleBridge.clearResult()
                                finish() // MainActivity 종료 → ComposeActivity(홈)로 복귀
                            },
                        )
                    }
                }
            }
        }
        addContentView(
            resultView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
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
