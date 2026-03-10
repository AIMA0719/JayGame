package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.jaygame.bridge.BattleBridge

/**
 * Renders active projectiles as beams/bolts between source and target.
 */
@Composable
fun ProjectileOverlay() {
    val projData by BattleBridge.projectiles.collectAsState()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = projData
        for (i in 0 until data.count) {
            val srcX = data.srcXs[i] * size.width
            val srcY = data.srcYs[i] * size.height
            val dstX = data.dstXs[i] * size.width
            val dstY = data.dstYs[i] * size.height
            val type = data.types[i]

            // Color based on projectile type
            val color: Color
            val glowColor: Color
            val width: Float
            when (type) {
                1 -> { color = Color(0xFFFF6B35); glowColor = Color(0xFFFF9800); width = 4f }  // Fire
                2 -> { color = Color(0xFF64B5F6); glowColor = Color(0xFF90CAF9); width = 3f }  // Ice
                3 -> { color = Color(0xFF81C784); glowColor = Color(0xFFA5D6A7); width = 2.5f } // Poison
                4 -> { color = Color(0xFFFFD54F); glowColor = Color(0xFFFFFF00); width = 3.5f } // Lightning
                else -> { color = Color.White; glowColor = Color(0xFFE0E0E0); width = 2f }      // Arrow
            }

            val src = Offset(srcX, srcY)
            val dst = Offset(dstX, dstY)

            // Glow line (wider, transparent)
            drawLine(
                color = glowColor.copy(alpha = 0.3f),
                start = src,
                end = dst,
                strokeWidth = width * 3f,
                cap = StrokeCap.Round,
            )

            // Main beam
            drawLine(
                color = color,
                start = src,
                end = dst,
                strokeWidth = width,
                cap = StrokeCap.Round,
            )

            // Core (bright center)
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = src,
                end = dst,
                strokeWidth = width * 0.4f,
                cap = StrokeCap.Round,
            )

            // Impact point glow at projectile position
            drawCircle(
                color = glowColor.copy(alpha = 0.4f),
                radius = width * 2.5f,
                center = src,
            )
        }
    }
}
