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
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LeatherBrown
import com.example.jaygame.ui.theme.MediumBrown

@Composable
fun WoodFrame(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(cornerRadius.toPx())
                val bw = borderWidth.toPx()

                // Background: vertical gradient from MediumBrown(0.95) to DarkBrown(0.98)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MediumBrown.copy(alpha = 0.95f),
                            DarkBrown.copy(alpha = 0.98f),
                        ),
                    ),
                    cornerRadius = cr,
                )

                // Border: LinearGradient of LeatherBrown -> DarkGold -> LeatherBrown
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(LeatherBrown, DarkGold, LeatherBrown),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = bw),
                )

                // Inner glow: Gold at 0.1 alpha, 1dp stroke
                val inset = bw + 1.dp.toPx()
                drawRoundRect(
                    color = Gold.copy(alpha = 0.1f),
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
