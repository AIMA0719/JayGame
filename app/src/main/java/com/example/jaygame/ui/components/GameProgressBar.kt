package com.example.jaygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LeatherBrown

@Composable
fun GameProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val cornerRadius = CornerRadius(size.height / 2)
        drawRoundRect(color = LeatherBrown, cornerRadius = cornerRadius)
        if (progress > 0f) {
            drawRoundRect(
                color = Gold,
                size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                cornerRadius = cornerRadius,
            )
        }
        drawRoundRect(color = DarkGold, cornerRadius = cornerRadius, style = Stroke(1.dp.toPx()))
    }
}
