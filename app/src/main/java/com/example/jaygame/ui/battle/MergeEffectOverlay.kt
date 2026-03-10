package com.example.jaygame.ui.battle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import kotlinx.coroutines.delay

/**
 * Shows a brief animation when merge completes:
 * - Normal merge: scale pulse + sparkle
 * - Lucky merge (jackpot): golden explosion + "JACKPOT!" text + screen flash
 * Auto-dismiss after ~1.2 seconds.
 */
@Composable
fun MergeEffectOverlay() {
    val effect by BattleBridge.mergeEffect.collectAsState()
    val data = effect ?: return

    // Auto-dismiss after 1.2 seconds
    LaunchedEffect(data) {
        delay(1200)
        BattleBridge.clearMergeEffect()
    }

    val scale by animateFloatAsState(
        targetValue = if (data.isLucky) 1.5f else 1.2f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "mergeScale",
    )

    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "mergeAlpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (data.isLucky) {
            // Lucky merge - golden flash overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Gold.copy(alpha = alpha * 0.3f)),
            )

            // JACKPOT text at center
            Text(
                text = "\u2605 JACKPOT! \u2605",
                color = Gold.copy(alpha = 1f - alpha * 0.5f),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
            )
        } else {
            // Normal merge - subtle sparkle
            Text(
                text = "\u2726",
                color = NeonCyan.copy(alpha = 1f - alpha),
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
            )
        }
    }
}
