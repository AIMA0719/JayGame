package com.example.jaygame.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.sin

// Pre-allocated colors
private val WaveTextColor = Color(0xFFFFD700)      // Gold
private val BossTextColor = Color(0xFFFF4444)       // Red
private val SubTextColor = Color.White.copy(alpha = 0.8f)
private val FlashWhite = Color.White
private val BarColor = Color(0xFFFFD700).copy(alpha = 0.6f)

/**
 * Shows "WAVE X" announcement when a new wave starts.
 * Boss waves get red text + warning effect.
 */
@Composable
fun WaveAnnouncementOverlay() {
    val battleState by BattleBridge.state.collectAsState()
    val currentWave = battleState.currentWave
    val isBoss = battleState.isBossRound
    val gameState = battleState.state // 0=WaveDelay, 1=Playing

    // Track wave transitions
    val lastAnnouncedWave = remember { mutableIntStateOf(-1) }

    // Animation values
    val textScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(0f) }

    // Trigger animation when wave changes to Playing state
    LaunchedEffect(currentWave, gameState) {
        if (gameState == 1 && currentWave != lastAnnouncedWave.intValue) {
            lastAnnouncedWave.intValue = currentWave

            // Reset
            textScale.snapTo(0f)
            textAlpha.snapTo(0f)
            flashAlpha.snapTo(0f)
            shakeOffset.snapTo(0f)

            // Screen flash (brief)
            flashAlpha.animateTo(0.3f, tween(50))
            flashAlpha.animateTo(0f, tween(200))

            // Text pop in
            textAlpha.animateTo(1f, tween(150))
            textScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))

            // Shake for boss
            if (isBoss) {
                repeat(6) {
                    shakeOffset.animateTo(8f * (if (it % 2 == 0) 1f else -1f), tween(40))
                }
                shakeOffset.animateTo(0f, tween(40))
            }

            // Hold
            kotlinx.coroutines.delay(800)

            // Fade out
            textAlpha.animateTo(0f, tween(400, easing = LinearEasing))
            textScale.snapTo(0f)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    if (textAlpha.value > 0.01f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val alpha = textAlpha.value
            val scale = textScale.value
            val shake = shakeOffset.value

            // Screen flash
            val fAlpha = flashAlpha.value
            if (fAlpha > 0f) {
                drawRect(color = FlashWhite.copy(alpha = fAlpha))
            }

            // Wave text
            val waveText = "WAVE $currentWave"
            val textColor = if (isBoss) BossTextColor else WaveTextColor
            val fontSize = if (isBoss) 52.sp else 44.sp

            val style = TextStyle(
                color = textColor.copy(alpha = alpha),
                fontSize = fontSize * scale,
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f * alpha),
                    offset = Offset(3f, 3f),
                    blurRadius = 8f,
                ),
            )

            val textLayout = textMeasurer.measure(waveText, style)
            val textX = (w - textLayout.size.width) / 2 + shake
            val textY = h * 0.38f

            drawText(textLayout, topLeft = Offset(textX, textY))

            // Sub text for boss
            if (isBoss && alpha > 0.3f) {
                val bossStyle = TextStyle(
                    color = SubTextColor.copy(alpha = (alpha - 0.3f).coerceIn(0f, 1f)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f,
                    ),
                )
                val bossLayout = textMeasurer.measure("⚠ BOSS WAVE ⚠", bossStyle)
                val bossX = (w - bossLayout.size.width) / 2
                val bossY = textY + textLayout.size.height + 8f
                drawText(bossLayout, topLeft = Offset(bossX, bossY))
            }

            // Decorative bars
            val barW = w * 0.4f * scale
            val barH = 2f
            val barY = textY - 12f
            val barY2 = textY + textLayout.size.height + (if (isBoss) 40f else 8f)

            drawRect(
                color = BarColor.copy(alpha = alpha * 0.6f),
                topLeft = Offset((w - barW) / 2, barY),
                size = androidx.compose.ui.geometry.Size(barW, barH),
            )
            drawRect(
                color = BarColor.copy(alpha = alpha * 0.6f),
                topLeft = Offset((w - barW) / 2, barY2),
                size = androidx.compose.ui.geometry.Size(barW, barH),
            )
        }
    }
}
