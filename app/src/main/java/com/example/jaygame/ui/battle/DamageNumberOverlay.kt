package com.example.jaygame.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.DamageEvent

/**
 * Shows floating damage numbers that rise and fade.
 * Uses Compose Animatable for smooth 60fps animation.
 */
@Composable
fun DamageNumberOverlay() {
    val showDamage by BattleBridge.showDamageNumbers.collectAsState()
    if (!showDamage) return

    val events by BattleBridge.damageEvents.collectAsState()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        if (containerSize.width > 0) {
            events.forEach { event ->
                key(event.id) {
                    AnimatedDamageNumber(
                        event = event,
                        containerWidth = containerSize.width,
                        containerHeight = containerSize.height,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedDamageNumber(
    event: DamageEvent,
    containerWidth: Int,
    containerHeight: Int,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(event.timestamp) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800),
        )
    }

    val p = progress.value
    val offsetY = -80f * p
    val alpha = (1f - p).coerceIn(0f, 1f)
    val scale = 1f + p * 0.4f

    val x = event.x * containerWidth
    val y = event.y * containerHeight + offsetY

    val density = LocalDensity.current
    val xDp = with(density) { x.toDp() }
    val yDp = with(density) { y.toDp() }

    Text(
        text = "-${event.damage}",
        color = if (event.isCrit) Color(0xFFFF5252) else Color(0xFFFFD54F),
        fontSize = if (event.isCrit) 18.sp else 14.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xDp - 20.dp, y = yDp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
    )
}
