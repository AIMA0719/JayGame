package com.example.jaygame.ui.components

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
import com.example.jaygame.ui.theme.BorderGlow
import com.example.jaygame.ui.theme.DarkNavy

// Legacy component — kept for screens not yet migrated. Use GameCard for new code.
@Composable
fun MedievalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderGlow,
    backgroundColor: Color = DarkNavy,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cr = CornerRadius(10.dp.toPx())
                drawRoundRect(color = backgroundColor, cornerRadius = cr)
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = cr,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(8.dp),
        content = content,
    )
}
