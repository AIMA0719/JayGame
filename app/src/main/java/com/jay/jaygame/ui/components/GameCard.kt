package com.jay.jaygame.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jay.jaygame.ui.theme.BorderGlow
import com.jay.jaygame.ui.theme.DarkNavy

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkNavy,
    borderColor: Color = BorderGlow,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())

                // Static glow layer for grade-based glow (no animation for perf in lazy lists)
                if (glowColor != null) {
                    val outerCr = CornerRadius(16.dp.toPx())
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.15f),
                        cornerRadius = outerCr,
                        style = Stroke(width = 4f.dp.toPx()),
                    )
                }

                drawRoundRect(color = backgroundColor, cornerRadius = cr)
                drawRoundRect(
                    color = if (glowColor != null) glowColor.copy(alpha = 0.65f) else borderColor,
                    cornerRadius = cr,
                    style = Stroke(width = if (glowColor != null) 1.5f.dp.toPx() else 1.dp.toPx()),
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        content = content,
    )
}
