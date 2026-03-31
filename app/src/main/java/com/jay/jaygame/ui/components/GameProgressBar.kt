package com.jay.jaygame.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.jaygame.ui.theme.BorderGlow
import com.jay.jaygame.ui.theme.DarkSurface
import com.jay.jaygame.ui.theme.Gold

// Pre-allocated shimmer colors
private val ShimmerWhiteLegacy = Color.White
private val ShimmerTransparentLegacy = Color.Transparent

// Legacy progress bar — use NeonProgressBar for new code.
@Composable
fun GameProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val transition = rememberInfiniteTransition(label = "legacyShimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "legacyShimmerOffset",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val cornerRadius = CornerRadius(size.height / 2)
        drawRoundRect(color = DarkSurface, cornerRadius = cornerRadius)
        if (clampedProgress > 0f) {
            val barWidth = size.width * clampedProgress
            drawRoundRect(
                color = Gold,
                size = Size(barWidth, size.height),
                cornerRadius = cornerRadius,
            )

            // Shimmer shine line
            val shimmerWidth = barWidth * 0.15f
            val shimmerX = barWidth * shimmerOffset
            if (shimmerX + shimmerWidth > 0f && shimmerX < barWidth) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ShimmerTransparentLegacy,
                            ShimmerWhiteLegacy.copy(alpha = 0.3f),
                            ShimmerWhiteLegacy.copy(alpha = 0.45f),
                            ShimmerWhiteLegacy.copy(alpha = 0.3f),
                            ShimmerTransparentLegacy,
                        ),
                        startX = shimmerX,
                        endX = shimmerX + shimmerWidth,
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                )
            }
        }
        drawRoundRect(color = BorderGlow, cornerRadius = cornerRadius, style = Stroke(1.dp.toPx()))
    }
}
