package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.NeonGreen

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    barColor: Color = NeonGreen,
    trackColor: Color = DeepDark,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val cr = CornerRadius(height.toPx() / 2)
                drawRoundRect(color = trackColor, cornerRadius = cr)
                if (clampedProgress > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor),
                        ),
                        cornerRadius = cr,
                        size = Size(size.width * clampedProgress, size.height),
                    )
                }
            },
    )
}
