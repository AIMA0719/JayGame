package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jaygame.ui.theme.BorderGlow
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.NeonCyan

// Legacy component — kept for screens not yet migrated. Use GameCard for new code.
@Composable
fun WoodFrame(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(cornerRadius.toPx())
                val bw = borderWidth.toPx()

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkNavy.copy(alpha = 0.95f),
                            DarkSurface.copy(alpha = 0.98f),
                        ),
                    ),
                    cornerRadius = cr,
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Divider, BorderGlow, Divider),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = bw),
                )

                val inset = bw + 1.dp.toPx()
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.05f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(
                        (cornerRadius.toPx() - inset).coerceAtLeast(0f),
                    ),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .padding(borderWidth + 2.dp),
        content = content,
    )
}
