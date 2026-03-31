package com.jay.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.engine.Grid

// Pre-allocated Color constants (avoid GC during Canvas draw)
private val DebugWaypointColor = Color.Cyan
private val DebugHomeMarkerColor = Color.Green
private val DebugPathLineColor = Color.Yellow.copy(alpha = 0.7f)
private val DebugGridBoundsColor = Color.White.copy(alpha = 0.5f)

// Grid constants from Grid.kt (world coords 720x1280)
private const val WORLD_W = 720f
private const val WORLD_H = 1280f

@Composable
fun DebugOverlay() {
    val debugMode by BattleBridge.debugMode.collectAsState()
    if (!debugMode) return

    // Read enemy path waypoints from engine
    val engine = BattleBridge.engine
    val waypoints = engine?.enemyPath ?: emptyList()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Helper: world coords to screen coords
        fun wx(worldX: Float) = worldX / WORLD_W * w
        fun wy(worldY: Float) = worldY / WORLD_H * h

        // ── a. Field area bounds (dashed) ──
        val originX = Grid.ORIGIN_X
        val originY = Grid.ORIGIN_Y

        val gridLeft = wx(originX)
        val gridTop = wy(originY)
        val gridRight = wx(originX + Grid.GRID_W)
        val gridBottom = wy(originY + Grid.GRID_H)
        drawRect(
            color = DebugGridBoundsColor,
            topLeft = Offset(gridLeft, gridTop),
            size = androidx.compose.ui.geometry.Size(gridRight - gridLeft, gridBottom - gridTop),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)),
            ),
        )

        // ── c. Field center marker ──
        val cx = wx(Grid.CENTER_X)
        val cy = wy(Grid.CENTER_Y)
        val armLen = 6f
        drawLine(DebugHomeMarkerColor, Offset(cx - armLen, cy), Offset(cx + armLen, cy), strokeWidth = 2f)
        drawLine(DebugHomeMarkerColor, Offset(cx, cy - armLen), Offset(cx, cy + armLen), strokeWidth = 2f)

        // ── b & d. Enemy path waypoints and connecting line ──
        if (waypoints.isNotEmpty()) {
            // d. Path line connecting waypoints in yellow
            for (i in 0 until waypoints.size - 1) {
                val from = waypoints[i]
                val to = waypoints[i + 1]
                drawLine(
                    color = DebugPathLineColor,
                    start = Offset(wx(from.x), wy(from.y)),
                    end = Offset(wx(to.x), wy(to.y)),
                    strokeWidth = 1.5f,
                )
            }
            // Close the loop (last to first)
            val last = waypoints.last()
            val first = waypoints.first()
            drawLine(
                color = DebugPathLineColor,
                start = Offset(wx(last.x), wy(last.y)),
                end = Offset(wx(first.x), wy(first.y)),
                strokeWidth = 1.5f,
            )

            // b. Waypoints as cyan dots
            for (wp in waypoints) {
                drawCircle(
                    color = DebugWaypointColor,
                    radius = 3f,
                    center = Offset(wx(wp.x), wy(wp.y)),
                )
            }
        }
    }
}
