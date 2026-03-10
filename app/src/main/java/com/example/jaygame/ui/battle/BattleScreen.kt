package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.ui.screens.ResultScreen

@Composable
fun BattleScreen(
    result: BattleResultData?,
    onGoHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A1A), Color(0xFF0F1528))
                )
            ),
    ) {
        // Layer 1: Game area with fixed 16:9 aspect ratio, centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1280f / 720f)
                .align(Alignment.Center),
        ) {
            // Order matters: path → field → enemies → projectiles → damage
            MonsterPathOverlay()
            BattleField()
            EnemyOverlay()
            ProjectileOverlay()
            DamageNumberOverlay()
        }

        // Layer 2: HUD overlays (on top of game)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BattleTopHud()
            Spacer(modifier = Modifier.weight(1f))
            BattleBottomHud()
        }

        // Layer 3: Popups
        UnitDetailPopup()
        MergeEffectOverlay()
        SummonEffectOverlay()

        // Layer 4: Result screen
        result?.let { data ->
            ResultScreen(
                victory = data.victory,
                waveReached = data.waveReached,
                goldEarned = data.goldEarned,
                trophyChange = data.trophyChange,
                killCount = data.killCount,
                mergeCount = data.mergeCount,
                onGoHome = onGoHome,
            )
        }
    }
}
