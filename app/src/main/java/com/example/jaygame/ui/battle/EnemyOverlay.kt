package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.jaygame.R
import com.example.jaygame.bridge.BattleBridge

// Pre-allocated color constants
private val HpBarBg = Color.Black.copy(alpha = 0.6f)
private val FallbackColors = arrayOf(
    Color(0xFFFF6B35),
    Color(0xFF64B5F6),
    Color(0xFF81C784),
    Color(0xFFFFD54F),
    Color(0xFFCE93D8),
)

/**
 * Renders all active enemies on a full-screen Canvas overlay.
 * Enemy positions come from C++ via BattleBridge as normalized coordinates (0-1).
 * Enemies are drawn as sprite icons with HP bars.
 * Positions are smoothly interpolated between updates for fluid 60fps movement.
 */
@Composable
fun EnemyOverlay() {
    val enemies by BattleBridge.enemyPositions.collectAsState()
    val context = LocalContext.current

    // Pre-load enemy bitmaps
    val enemyBitmaps = remember {
        val drawableIds = mapOf(
            0 to R.drawable.ic_enemy_0,
            1 to R.drawable.ic_enemy_1,
            2 to R.drawable.ic_enemy_2,
            3 to R.drawable.ic_enemy_3,
            4 to R.drawable.ic_enemy_4,
            99 to R.drawable.ic_enemy_boss,
        )
        drawableIds.mapValues { (_, resId) ->
            ContextCompat.getDrawable(context, resId)?.toBitmap(48, 48)?.asImageBitmap()
        }
    }

    val bossBitmap = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_enemy_boss)?.toBitmap(72, 72)?.asImageBitmap()
    }

    // Smooth interpolated positions
    val smoothXs = remember { mutableStateOf(FloatArray(0)) }
    val smoothYs = remember { mutableStateOf(FloatArray(0)) }

    // Continuously lerp toward target positions at display frame rate
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val data = BattleBridge.enemyPositions.value
                val sx = smoothXs.value
                val sy = smoothYs.value

                if (data.count != sx.size) {
                    // Enemy count changed — snap to new positions
                    smoothXs.value = data.xs.copyOf(data.count)
                    smoothYs.value = data.ys.copyOf(data.count)
                } else if (data.count > 0) {
                    // Lerp toward target positions
                    val lerpFactor = 0.2f
                    val newX = FloatArray(data.count)
                    val newY = FloatArray(data.count)
                    for (i in 0 until data.count) {
                        newX[i] = sx[i] + (data.xs[i] - sx[i]) * lerpFactor
                        newY[i] = sy[i] + (data.ys[i] - sy[i]) * lerpFactor
                    }
                    smoothXs.value = newX
                    smoothYs.value = newY
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = enemies
        val sxArr = smoothXs.value
        val syArr = smoothYs.value

        // Use smooth positions if sizes match, otherwise fall back to raw
        val useSmooth = sxArr.size == data.count && data.count > 0

        for (i in 0 until data.count) {
            val screenX = if (useSmooth) sxArr[i] * size.width else data.xs[i] * size.width
            val screenY = if (useSmooth) syArr[i] * size.height else data.ys[i] * size.height
            val type = data.types[i]
            val hpRatio = data.hpRatios[i]
            val isBoss = type == 99

            val spriteSize = if (isBoss) 54f else 36f
            val bitmap = if (isBoss) bossBitmap else enemyBitmaps[type % 5]

            // Draw enemy sprite
            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    topLeft = Offset(screenX - spriteSize / 2, screenY - spriteSize / 2),
                )
            } else {
                // Fallback: colored circle
                val color = FallbackColors[type % 5]
                drawCircle(
                    color = color,
                    radius = spriteSize / 2,
                    center = Offset(screenX, screenY),
                )
            }

            // HP bar
            val barWidth = spriteSize * 1.2f
            val barHeight = 4f
            val barX = screenX - barWidth / 2
            val barY = screenY - spriteSize / 2 - 8f

            // Background
            drawRect(
                color = HpBarBg,
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight),
            )
            // HP fill
            val hpColor = Color(
                red = 1f - hpRatio,
                green = hpRatio,
                blue = 0.1f,
                alpha = 0.9f,
            )
            drawRect(
                color = hpColor,
                topLeft = Offset(barX, barY),
                size = Size(barWidth * hpRatio.coerceIn(0f, 1f), barHeight),
            )
        }
    }
}
