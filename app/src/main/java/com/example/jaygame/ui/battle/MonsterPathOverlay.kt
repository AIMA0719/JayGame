package com.example.jaygame.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.STAGES
import kotlin.math.sin

// Pre-allocated constants
private val DashLineColor = Color.White.copy(alpha = 0.1f)
private val OuterStroke = Stroke(width = 2.5f)
private val InnerStroke = Stroke(width = 1.5f)
private val ArrowColor = Color.White.copy(alpha = 0.15f)

// Cliff/elevation colors
private val CliffDark = Color(0xFF1A0F08)
private val CliffMid = Color(0xFF2A1A0E)
private val CliffHighlight = Color.White.copy(alpha = 0.12f)
private val CliffEdgeStroke = Stroke(width = 1f)
private val GroundShadow = Color.Black.copy(alpha = 0.35f)

// Path tile colors
private val TileBright = Color.White.copy(alpha = 0.06f)
private val TileDim = Color.Black.copy(alpha = 0.06f)
private val TileBorder = Color.Black.copy(alpha = 0.08f)

// Ground surface detail
private val GroundHighlight = Color.White.copy(alpha = 0.08f)
private val GroundInnerShadow = Color.Black.copy(alpha = 0.15f)
private val GroundEdgeLight = Color.White.copy(alpha = 0.15f)
private val GroundEdgeDark = Color.Black.copy(alpha = 0.2f)

// Coordinates in 1280x720 space — matching Grid.kt (480x480 centered)
private const val GRID_LEFT = 400f
private const val GRID_TOP = 120f
private const val GRID_RIGHT = 880f
private const val GRID_BOTTOM = 600f
private const val PATH_MARGIN = 60f
private const val PATH_LEFT = GRID_LEFT - PATH_MARGIN     // 340
private const val PATH_TOP = GRID_TOP - PATH_MARGIN       // 60
private const val PATH_RIGHT = GRID_RIGHT + PATH_MARGIN   // 940
private const val PATH_BOTTOM = GRID_BOTTOM + PATH_MARGIN // 660

// Cliff depth in pixels (1280x720 space)
private const val CLIFF_DEPTH = 25f

/**
 * Draws the battlefield: monster path (stone tiles) + unit ground (elevated cliff platform).
 * All rendering is Canvas-only, no image assets needed.
 */
@Composable
fun MonsterPathOverlay() {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }

    // Stage-dependent colors
    val pathColor = remember(stageId) { stage.pathColor }
    val pathColorBright = remember(stageId) { stage.pathColor.copy(alpha = 0.7f) }
    val pathColorDim = remember(stageId) { stage.pathColor.copy(alpha = 0.4f) }
    val pathBorderColor = remember(stageId) { stage.pathColor.copy(alpha = 0.5f) }
    val bgColorLast = remember(stageId) { stage.bgColors.last() }
    val fieldTop = remember(stageId) { stage.fieldColors.first() }
    val fieldMid = remember(stageId) { stage.fieldColors.getOrElse(1) { stage.fieldColors.first() } }
    val fieldBot = remember(stageId) { stage.fieldColors.last() }
    val cliffColor = remember(stageId) {
        Color(
            red = (fieldBot.red * 0.5f).coerceIn(0f, 1f),
            green = (fieldBot.green * 0.5f).coerceIn(0f, 1f),
            blue = (fieldBot.blue * 0.5f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
    val cliffDarker = remember(stageId) {
        Color(
            red = (fieldBot.red * 0.3f).coerceIn(0f, 1f),
            green = (fieldBot.green * 0.3f).coerceIn(0f, 1f),
            blue = (fieldBot.blue * 0.3f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }

    // Animated dash for path flow
    val infiniteTransition = rememberInfiniteTransition(label = "pathFlow")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashOffset",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Convert 1280x720 → screen coords
        val pL = (PATH_LEFT / 1280f) * w
        val pT = (PATH_TOP / 720f) * h
        val pR = (PATH_RIGHT / 1280f) * w
        val pB = (PATH_BOTTOM / 720f) * h
        val gL = (GRID_LEFT / 1280f) * w
        val gT = (GRID_TOP / 720f) * h
        val gR = (GRID_RIGHT / 1280f) * w
        val gB = (GRID_BOTTOM / 720f) * h
        val pW = pR - pL
        val pH = pB - pT
        val gW = gR - gL
        val gH = gB - gT
        val cliffH = (CLIFF_DEPTH / 720f) * h
        val cornerR = 12f

        // ═══════════════════════════════════════════
        // 1. MONSTER PATH (stone tile road)
        // ═══════════════════════════════════════════

        // Path fill
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(pathColorBright, pathColorDim)),
            topLeft = Offset(pL, pT),
            size = Size(pW, pH),
            cornerRadius = CornerRadius(16f),
        )

        // Stone tile pattern on path
        drawPathTiles(pL, pT, pR, pB, gL, gT, gR, gB, pathColor)

        // Outer path border
        drawRoundRect(
            color = pathBorderColor,
            topLeft = Offset(pL, pT),
            size = Size(pW, pH),
            cornerRadius = CornerRadius(16f),
            style = OuterStroke,
        )

        // ═══════════════════════════════════════════
        // 2. ELEVATED GROUND (cliff platform)
        // ═══════════════════════════════════════════

        // 2a. Drop shadow under platform
        drawRoundRect(
            color = GroundShadow,
            topLeft = Offset(gL - 4f, gB + 2f),
            size = Size(gW + 8f, cliffH + 8f),
            cornerRadius = CornerRadius(cornerR + 4f),
        )

        // 2b. Cliff side (bottom face) — trapezoid for depth
        val cliffPath = Path().apply {
            moveTo(gL + 4f, gB)           // left of bottom edge
            lineTo(gR - 4f, gB)           // right of bottom edge
            lineTo(gR + 2f, gB + cliffH)  // slightly wider at bottom
            lineTo(gL - 2f, gB + cliffH)
            close()
        }
        drawPath(cliffPath, cliffColor)

        // Cliff vertical stripes (stone texture)
        val stripeCount = 16
        for (s in 0 until stripeCount) {
            val frac = (s + 0.5f) / stripeCount
            val sx = gL + gW * frac
            val topY = gB + 2f
            val botY = gB + cliffH - 2f
            val stripeAlpha = if (s % 2 == 0) 0.08f else 0.04f
            drawLine(
                color = Color.Black.copy(alpha = stripeAlpha),
                start = Offset(sx, topY),
                end = Offset(sx, botY),
                strokeWidth = 1f,
            )
        }

        // Cliff bottom edge (darkest)
        drawLine(
            color = cliffDarker,
            start = Offset(gL - 2f, gB + cliffH),
            end = Offset(gR + 2f, gB + cliffH),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        // 2c. Right cliff side (thinner)
        val rightCliffW = cliffH * 0.4f
        val rightCliffPath = Path().apply {
            moveTo(gR, gT + 8f)
            lineTo(gR + rightCliffW, gT + 8f + rightCliffW * 0.5f)
            lineTo(gR + rightCliffW, gB + cliffH - 4f)
            lineTo(gR, gB)
            close()
        }
        drawPath(rightCliffPath, cliffDarker)

        // 2d. Ground top surface
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(fieldTop, fieldMid, fieldBot),
            ),
            topLeft = Offset(gL, gT),
            size = Size(gW, gH),
            cornerRadius = CornerRadius(cornerR),
        )

        // Ground surface highlight (top edge - light hits from above)
        drawRoundRect(
            color = GroundEdgeLight,
            topLeft = Offset(gL, gT),
            size = Size(gW, 2f),
            cornerRadius = CornerRadius(cornerR),
        )
        // Left edge highlight
        drawLine(
            color = GroundHighlight,
            start = Offset(gL + 1f, gT + cornerR),
            end = Offset(gL + 1f, gB - cornerR),
            strokeWidth = 1f,
        )

        // Ground inner shadow (bottom + right edges)
        drawLine(
            color = GroundInnerShadow,
            start = Offset(gL + cornerR, gB - 1f),
            end = Offset(gR - cornerR, gB - 1f),
            strokeWidth = 2f,
        )
        drawLine(
            color = GroundInnerShadow,
            start = Offset(gR - 1f, gT + cornerR),
            end = Offset(gR - 1f, gB - cornerR),
            strokeWidth = 2f,
        )

        // Ground border
        drawRoundRect(
            color = GroundEdgeDark,
            topLeft = Offset(gL, gT),
            size = Size(gW, gH),
            cornerRadius = CornerRadius(cornerR),
            style = InnerStroke,
        )

        // Ground subtle texture (grid-like subtle lines)
        val cellSize = gW / 5f
        for (i in 1 until 5) {
            val lx = gL + cellSize * i
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(lx, gT + 8f),
                end = Offset(lx, gB - 8f),
                strokeWidth = 0.5f,
            )
            val ly = gT + cellSize * i
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(gL + 8f, ly),
                end = Offset(gR - 8f, ly),
                strokeWidth = 0.5f,
            )
        }

        // ═══════════════════════════════════════════
        // 3. PATH FLOW INDICATORS
        // ═══════════════════════════════════════════

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), dashOffset)
        val reverseDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 40f - dashOffset)

        val midTopY = (pT + gT) / 2
        val midRightX = (gR + pR) / 2
        val midBottomY = (gB + pB) / 2
        val midLeftX = (pL + gL) / 2

        // Dashed flow lines
        drawLine(DashLineColor, Offset(pL + 20f, midTopY), Offset(pR - 20f, midTopY),
            strokeWidth = 1.5f, pathEffect = dashEffect)
        drawLine(DashLineColor, Offset(midRightX, pT + 20f), Offset(midRightX, pB - 20f),
            strokeWidth = 1.5f, pathEffect = dashEffect)
        drawLine(DashLineColor, Offset(pR - 20f, midBottomY), Offset(pL + 20f, midBottomY),
            strokeWidth = 1.5f, pathEffect = reverseDashEffect)
        drawLine(DashLineColor, Offset(midLeftX, pB - 20f), Offset(midLeftX, pT + 20f),
            strokeWidth = 1.5f, pathEffect = reverseDashEffect)

        // Direction arrows
        val arrowSize = 6f
        val arrowCount = 5
        drawDirectionArrows(pL, pT, pR, pB, midTopY, midRightX, midBottomY, midLeftX,
            arrowSize, arrowCount, dashOffset)
    }
}

/**
 * Draws stone tile pattern on the monster path (the strip between outer and inner rects).
 */
private fun DrawScope.drawPathTiles(
    pL: Float, pT: Float, pR: Float, pB: Float,
    gL: Float, gT: Float, gR: Float, gB: Float,
    pathColor: Color,
) {
    val stripW = gL - pL  // path strip width (~60px scaled)
    val tileSize = stripW / 2  // 2 tiles across the strip width

    if (tileSize < 4f) return

    // Top strip tiles
    var tx = pL + 4f
    while (tx < pR - 4f) {
        var ty = pT + 4f
        while (ty < gT - 2f) {
            val col = ((tx / tileSize).toInt() + (ty / tileSize).toInt()) % 2
            val tColor = if (col == 0) TileBright else TileDim
            val tw = tileSize.coerceAtMost(pR - 4f - tx)
            val th = tileSize.coerceAtMost(gT - 2f - ty)
            drawRect(color = tColor, topLeft = Offset(tx, ty), size = Size(tw, th))
            drawRect(color = TileBorder, topLeft = Offset(tx, ty), size = Size(tw, th),
                style = Stroke(width = 0.5f))
            ty += tileSize
        }
        tx += tileSize
    }

    // Bottom strip tiles
    tx = pL + 4f
    while (tx < pR - 4f) {
        var ty = gB + 2f
        while (ty < pB - 4f) {
            val col = ((tx / tileSize).toInt() + (ty / tileSize).toInt()) % 2
            val tColor = if (col == 0) TileBright else TileDim
            val tw = tileSize.coerceAtMost(pR - 4f - tx)
            val th = tileSize.coerceAtMost(pB - 4f - ty)
            drawRect(color = tColor, topLeft = Offset(tx, ty), size = Size(tw, th))
            drawRect(color = TileBorder, topLeft = Offset(tx, ty), size = Size(tw, th),
                style = Stroke(width = 0.5f))
            ty += tileSize
        }
        tx += tileSize
    }

    // Left strip tiles
    var ly = gT
    while (ly < gB) {
        var lx = pL + 4f
        while (lx < gL - 2f) {
            val col = ((lx / tileSize).toInt() + (ly / tileSize).toInt()) % 2
            val tColor = if (col == 0) TileBright else TileDim
            val tw = tileSize.coerceAtMost(gL - 2f - lx)
            val th = tileSize.coerceAtMost(gB - ly)
            drawRect(color = tColor, topLeft = Offset(lx, ly), size = Size(tw, th))
            drawRect(color = TileBorder, topLeft = Offset(lx, ly), size = Size(tw, th),
                style = Stroke(width = 0.5f))
            lx += tileSize
        }
        ly += tileSize
    }

    // Right strip tiles
    ly = gT
    while (ly < gB) {
        var rx = gR + 2f
        while (rx < pR - 4f) {
            val col = ((rx / tileSize).toInt() + (ly / tileSize).toInt()) % 2
            val tColor = if (col == 0) TileBright else TileDim
            val tw = tileSize.coerceAtMost(pR - 4f - rx)
            val th = tileSize.coerceAtMost(gB - ly)
            drawRect(color = tColor, topLeft = Offset(rx, ly), size = Size(tw, th))
            drawRect(color = TileBorder, topLeft = Offset(rx, ly), size = Size(tw, th),
                style = Stroke(width = 0.5f))
            rx += tileSize
        }
        ly += tileSize
    }
}

/**
 * Draws directional arrows along the path.
 */
private fun DrawScope.drawDirectionArrows(
    pL: Float, pT: Float, pR: Float, pB: Float,
    midTopY: Float, midRightX: Float, midBottomY: Float, midLeftX: Float,
    arrowSize: Float, arrowCount: Int, dashOffset: Float,
) {
    // Top: left → right
    for (i in 0 until arrowCount) {
        val frac = (i + 0.5f) / arrowCount
        val phase = (frac + dashOffset / 40f) % 1f
        val ax = pL + 30f + (pR - pL - 60f) * frac
        val ay = midTopY
        val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay - arrowSize), Offset(ax, ay),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax - arrowSize, ay + arrowSize),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
    }

    // Right: top → bottom
    for (i in 0 until arrowCount) {
        val frac = (i + 0.5f) / arrowCount
        val phase = (frac + dashOffset / 40f) % 1f
        val ax = midRightX
        val ay = pT + 30f + (pB - pT - 60f) * frac
        val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay - arrowSize), Offset(ax, ay),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay - arrowSize),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
    }

    // Bottom: right → left
    for (i in 0 until arrowCount) {
        val frac = (i + 0.5f) / arrowCount
        val phase = (frac + dashOffset / 40f) % 1f
        val ax = pR - 30f - (pR - pL - 60f) * frac
        val ay = midBottomY
        val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax + arrowSize, ay - arrowSize), Offset(ax, ay),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay + arrowSize),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
    }

    // Left: bottom → top
    for (i in 0 until arrowCount) {
        val frac = (i + 0.5f) / arrowCount
        val phase = (frac + dashOffset / 40f) % 1f
        val ax = midLeftX
        val ay = pB - 30f - (pB - pT - 60f) * frac
        val alpha = (0.15f + sin(phase * 3.14f) * 0.1f).coerceIn(0.05f, 0.3f)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax - arrowSize, ay + arrowSize), Offset(ax, ay),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(ArrowColor.copy(alpha = alpha), Offset(ax, ay), Offset(ax + arrowSize, ay + arrowSize),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
}
