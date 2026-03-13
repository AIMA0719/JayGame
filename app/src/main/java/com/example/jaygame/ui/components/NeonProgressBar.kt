package com.example.jaygame.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.NeonGreen

// Pre-allocated shimmer colors
private val ShimmerWhite = Color.White
private val ShimmerTransparent = Color.Transparent

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    barColor: Color = NeonGreen,
    trackColor: Color = DeepDark,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Shimmer animation: a diagonal bright line moving across the bar
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .drawBehind {
                val cr = CornerRadius(height.toPx() / 2)
                drawRoundRect(color = trackColor, cornerRadius = cr)
                if (clampedProgress > 0f) {
                    val barWidth = size.width * clampedProgress
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor),
                        ),
                        cornerRadius = cr,
                        size = Size(barWidth, size.height),
                    )

                    // Draw shimmer shine line over the filled portion
                    val shimmerWidth = barWidth * 0.15f
                    val shimmerX = barWidth * shimmerOffset
                    if (shimmerX + shimmerWidth > 0f && shimmerX < barWidth) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    ShimmerTransparent,
                                    ShimmerWhite.copy(alpha = 0.35f),
                                    ShimmerWhite.copy(alpha = 0.5f),
                                    ShimmerWhite.copy(alpha = 0.35f),
                                    ShimmerTransparent,
                                ),
                                startX = shimmerX,
                                endX = shimmerX + shimmerWidth,
                            ),
                            topLeft = Offset(0f, 0f),
                            size = Size(barWidth, size.height),
                        )
                    }
                }
            },
    )
}
