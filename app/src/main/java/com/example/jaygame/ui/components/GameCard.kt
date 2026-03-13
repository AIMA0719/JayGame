package com.example.jaygame.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.BorderGlow
import com.example.jaygame.ui.theme.DarkNavy

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkNavy,
    borderColor: Color = BorderGlow,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // Pulsing glow animation when glowColor is provided
    val glowAlpha = if (glowColor != null) {
        val transition = rememberInfiniteTransition(label = "cardGlow")
        val pulse by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "cardGlowAlpha",
        )
        pulse
    } else 0f

    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())

                // Outer glow layer for grade-based glow
                if (glowColor != null) {
                    val outerCr = CornerRadius(16.dp.toPx())
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.3f),
                        cornerRadius = outerCr,
                        style = Stroke(width = (6f + glowAlpha * 4f).dp.toPx()),
                    )
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.15f),
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = (3f + glowAlpha * 2f).dp.toPx()),
                    )
                }

                drawRoundRect(color = backgroundColor, cornerRadius = cr)
                drawRoundRect(
                    color = if (glowColor != null) glowColor.copy(alpha = 0.5f + glowAlpha * 0.3f) else borderColor,
                    cornerRadius = cr,
                    style = Stroke(width = if (glowColor != null) 1.5f.dp.toPx() else 1.dp.toPx()),
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        content = content,
    )
}
