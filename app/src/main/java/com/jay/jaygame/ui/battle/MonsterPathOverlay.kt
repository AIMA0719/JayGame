package com.jay.jaygame.ui.battle

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.data.STAGES
import com.jay.jaygame.engine.Grid
import kotlin.math.abs
import kotlin.math.sin

// Pre-allocated constants
private val OuterStroke = Stroke(width = 2.5f)
private val InnerStroke = Stroke(width = 1.5f)

// Z8: Corner fill colors
private val CornerFillDark = Color.Black.copy(alpha = 0.08f)
private val CornerFillLight = Color.White.copy(alpha = 0.04f)

// Z9: Light particle colors
private val ParticleBright = Color.White.copy(alpha = 0.35f)
private val ParticleMid = Color.White.copy(alpha = 0.2f)
private val ParticleDim = Color.White.copy(alpha = 0.1f)

// Z10: Stone/pebble colors
private val StoneColor1 = Color.White.copy(alpha = 0.1f)
private val StoneColor2 = Color.Black.copy(alpha = 0.12f)
private val StoneColor3 = Color.White.copy(alpha = 0.06f)


// Path tile colors
private val TileBright = Color.White.copy(alpha = 0.06f)
private val TileDim = Color.Black.copy(alpha = 0.06f)
private val TileBorder = Color.Black.copy(alpha = 0.08f)

// Ground surface detail
private val GroundEdgeLight = Color.White.copy(alpha = 0.15f)
private val GroundEdgeDark = Color.Black.copy(alpha = 0.2f)

// Coordinates in 720x1280 space — derived from Grid.kt constants
private val GRID_LEFT = Grid.ORIGIN_X                             // 72
private val GRID_TOP = Grid.ORIGIN_Y                              // 400
private val GRID_RIGHT = Grid.ORIGIN_X + Grid.GRID_W             // 648
private val GRID_BOTTOM = Grid.ORIGIN_Y + Grid.GRID_H            // 760
private const val PATH_MARGIN_SIDE = 66f  // 좌우 마진 (확대 비례)
private const val PATH_MARGIN_TB = 76f    // 상하 마진 (확대 비례)
private val PATH_LEFT = GRID_LEFT - PATH_MARGIN_SIDE              // 20
private val PATH_TOP = GRID_TOP - PATH_MARGIN_TB                  // 340
private val PATH_RIGHT = GRID_RIGHT + PATH_MARGIN_SIDE            // 700
private val PATH_BOTTOM = GRID_BOTTOM + PATH_MARGIN_TB            // 820

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

        // Convert 720x1280 → screen coords
        val scaleX = w / 720f
        val scaleY = h / 1280f
        val pL = PATH_LEFT * scaleX
        val pT = PATH_TOP * scaleY
        val pR = PATH_RIGHT * scaleX
        val pB = PATH_BOTTOM * scaleY
        val gL = GRID_LEFT * scaleX
        val gT = GRID_TOP * scaleY
        val gR = GRID_RIGHT * scaleX
        val gB = GRID_BOTTOM * scaleY
        val pW = pR - pL
        val pH = pB - pT
        val gW = gR - gL
        val gH = gB - gT
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
        // 2. SPAWN GROUND (flat with subtle bottom shadow for depth)
        // ═══════════════════════════════════════════

        // Main ground surface
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(fieldTop, fieldMid, fieldBot),
            ),
            topLeft = Offset(gL, gT),
            size = Size(gW, gH),
            cornerRadius = CornerRadius(cornerR),
        )

        // Bottom shadow strip for subtle 3D depth (like reference image)
        val shadowH = gH * 0.06f  // ~6% of ground height
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(gL, gB - shadowH),
            size = Size(gW, shadowH + cornerR),
            cornerRadius = CornerRadius(cornerR),
        )

        // Top edge highlight
        drawRoundRect(
            color = GroundEdgeLight,
            topLeft = Offset(gL, gT),
            size = Size(gW, 2f),
            cornerRadius = CornerRadius(cornerR),
        )

        // Border
        drawRoundRect(
            color = GroundEdgeDark,
            topLeft = Offset(gL, gT),
            size = Size(gW, gH),
            cornerRadius = CornerRadius(cornerR),
            style = InnerStroke,
        )

        // Ground subtle texture (no grid lines — free-form placement)

        // ═══════════════════════════════════════════
        // 3. CORNER VISUAL TREATMENT (Z8)
        // ═══════════════════════════════════════════
        drawCornerFills(pL, pT, pR, pB, gL, gT, gR, gB)

        // ═══════════════════════════════════════════
        // 4. ANIMATED LIGHT PARTICLES (Z9)
        // ═══════════════════════════════════════════
        val midTopY = (pT + gT) / 2
        val midRightX = (gR + pR) / 2
        val midBottomY = (gB + pB) / 2
        val midLeftX = (pL + gL) / 2

        drawFlowParticles(pL, pT, pR, pB, midTopY, midRightX, midBottomY, midLeftX, dashOffset)

        // ═══════════════════════════════════════════
        // 5. PATH EDGE STONE DECORATIONS (Z10)
        // ═══════════════════════════════════════════
        drawEdgeStones(pL, pT, pR, pB, gL, gT, gR, gB)
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
 * Z8: Fills the 4 corner areas where the path turns with smooth rounded shapes.
 * Corners are the areas between the outer path rect and the inner grid rect at each corner.
 */
private fun DrawScope.drawCornerFills(
    pL: Float, pT: Float, pR: Float, pB: Float,
    gL: Float, gT: Float, gR: Float, gB: Float,
) {
    val cornerRadius = 20f

    // Top-left corner: quarter arc from top strip into left strip
    val tlPath = Path().apply {
        moveTo(pL + 16f, gT)
        lineTo(gL, gT)
        lineTo(gL, pT + 16f)
        // Arc curving inward
        cubicTo(gL, pT + 16f, pL + 16f + cornerRadius * 0.2f, pT + 16f + cornerRadius * 0.2f, pL + 16f, gT)
        close()
    }
    drawPath(tlPath, CornerFillDark)
    // Inner highlight arc
    val tlHighlight = Path().apply {
        moveTo(pL + 18f, gT - 2f)
        cubicTo(pL + 18f, pT + 18f + cornerRadius * 0.3f, gL - 2f, pT + 18f, gL - 2f, pT + 18f)
    }
    drawPath(tlHighlight, CornerFillLight, style = Stroke(width = 2f, cap = StrokeCap.Round))

    // Top-right corner
    val trPath = Path().apply {
        moveTo(gR, pT + 16f)
        lineTo(gR, gT)
        lineTo(pR - 16f, gT)
        cubicTo(pR - 16f, gT, pR - 16f - cornerRadius * 0.2f, pT + 16f + cornerRadius * 0.2f, gR, pT + 16f)
        close()
    }
    drawPath(trPath, CornerFillDark)
    val trHighlight = Path().apply {
        moveTo(gR + 2f, pT + 18f)
        cubicTo(gR + 2f, pT + 18f, pR - 18f, pT + 18f + cornerRadius * 0.3f, pR - 18f, gT - 2f)
    }
    drawPath(trHighlight, CornerFillLight, style = Stroke(width = 2f, cap = StrokeCap.Round))

    // Bottom-right corner
    val brPath = Path().apply {
        moveTo(pR - 16f, gB)
        lineTo(gR, gB)
        lineTo(gR, pB - 16f)
        cubicTo(gR, pB - 16f, pR - 16f - cornerRadius * 0.2f, pB - 16f - cornerRadius * 0.2f, pR - 16f, gB)
        close()
    }
    drawPath(brPath, CornerFillDark)
    val brHighlight = Path().apply {
        moveTo(pR - 18f, gB + 2f)
        cubicTo(pR - 18f, gB + 2f, pR - 18f - cornerRadius * 0.3f, pB - 18f, gR + 2f, pB - 18f)
    }
    drawPath(brHighlight, CornerFillLight, style = Stroke(width = 2f, cap = StrokeCap.Round))

    // Bottom-left corner
    val blPath = Path().apply {
        moveTo(gL, pB - 16f)
        lineTo(gL, gB)
        lineTo(pL + 16f, gB)
        cubicTo(pL + 16f, gB, pL + 16f + cornerRadius * 0.2f, pB - 16f - cornerRadius * 0.2f, gL, pB - 16f)
        close()
    }
    drawPath(blPath, CornerFillDark)
    val blHighlight = Path().apply {
        moveTo(gL - 2f, pB - 18f)
        cubicTo(gL - 2f, pB - 18f, pL + 18f + cornerRadius * 0.3f, pB - 18f, pL + 18f, gB + 2f)
    }
    drawPath(blHighlight, CornerFillLight, style = Stroke(width = 2f, cap = StrokeCap.Round))
}

/**
 * Z9: Animated light particles flowing along the path direction.
 * Replaces the old dash lines and direction arrows.
 */
private fun DrawScope.drawFlowParticles(
    pL: Float, pT: Float, pR: Float, pB: Float,
    midTopY: Float, midRightX: Float, midBottomY: Float, midLeftX: Float,
    animOffset: Float,
) {
    val particleCount = 8
    val normalizedOffset = animOffset / 40f  // 0..1 animated cycle

    // Top strip: left → right
    for (i in 0 until particleCount) {
        val baseFrac = i.toFloat() / particleCount
        val frac = (baseFrac + normalizedOffset) % 1f
        val px = pL + 20f + (pR - pL - 40f) * frac
        val py = midTopY
        val fadeAlpha = sin(frac * 3.1416f)  // fade in/out at edges
        val radius = 1.5f + fadeAlpha * 1.5f
        val color = when {
            fadeAlpha > 0.7f -> ParticleBright
            fadeAlpha > 0.3f -> ParticleMid
            else -> ParticleDim
        }
        drawCircle(color = color, radius = radius, center = Offset(px, py))
        // Small trailing glow
        if (frac > 0.02f) {
            drawCircle(color = ParticleDim, radius = radius * 0.6f,
                center = Offset(px - (pR - pL) * 0.015f, py))
        }
    }

    // Right strip: top → bottom
    for (i in 0 until particleCount) {
        val baseFrac = i.toFloat() / particleCount
        val frac = (baseFrac + normalizedOffset) % 1f
        val px = midRightX
        val py = pT + 20f + (pB - pT - 40f) * frac
        val fadeAlpha = sin(frac * 3.1416f)
        val radius = 1.5f + fadeAlpha * 1.5f
        val color = when {
            fadeAlpha > 0.7f -> ParticleBright
            fadeAlpha > 0.3f -> ParticleMid
            else -> ParticleDim
        }
        drawCircle(color = color, radius = radius, center = Offset(px, py))
        if (frac > 0.02f) {
            drawCircle(color = ParticleDim, radius = radius * 0.6f,
                center = Offset(px, py - (pB - pT) * 0.015f))
        }
    }

    // Bottom strip: right → left
    for (i in 0 until particleCount) {
        val baseFrac = i.toFloat() / particleCount
        val frac = (baseFrac + normalizedOffset) % 1f
        val px = pR - 20f - (pR - pL - 40f) * frac
        val py = midBottomY
        val fadeAlpha = sin(frac * 3.1416f)
        val radius = 1.5f + fadeAlpha * 1.5f
        val color = when {
            fadeAlpha > 0.7f -> ParticleBright
            fadeAlpha > 0.3f -> ParticleMid
            else -> ParticleDim
        }
        drawCircle(color = color, radius = radius, center = Offset(px, py))
        if (frac > 0.02f) {
            drawCircle(color = ParticleDim, radius = radius * 0.6f,
                center = Offset(px + (pR - pL) * 0.015f, py))
        }
    }

    // Left strip: bottom → top
    for (i in 0 until particleCount) {
        val baseFrac = i.toFloat() / particleCount
        val frac = (baseFrac + normalizedOffset) % 1f
        val px = midLeftX
        val py = pB - 20f - (pB - pT - 40f) * frac
        val fadeAlpha = sin(frac * 3.1416f)
        val radius = 1.5f + fadeAlpha * 1.5f
        val color = when {
            fadeAlpha > 0.7f -> ParticleBright
            fadeAlpha > 0.3f -> ParticleMid
            else -> ParticleDim
        }
        drawCircle(color = color, radius = radius, center = Offset(px, py))
        if (frac > 0.02f) {
            drawCircle(color = ParticleDim, radius = radius * 0.6f,
                center = Offset(px, py + (pB - pT) * 0.015f))
        }
    }
}

/**
 * Z10: Small stone/pebble decorations along the outer and inner edges of the path.
 * Uses deterministic pseudo-random placement based on position hashing.
 */
private fun DrawScope.drawEdgeStones(
    pL: Float, pT: Float, pR: Float, pB: Float,
    gL: Float, gT: Float, gR: Float, gB: Float,
) {
    val stoneSpacing = 18f
    val maxStoneRadius = 2.5f
    val minStoneRadius = 1.0f

    // Helper: deterministic pseudo-random from position
    fun hash(x: Float, y: Float, seed: Int): Float {
        val v = (x * 73.137f + y * 131.29f + seed * 47.53f)
        return abs(sin(v.toDouble()).toFloat()) // 0..1
    }

    fun stoneColor(h: Float): Color = when {
        h < 0.33f -> StoneColor1
        h < 0.66f -> StoneColor2
        else -> StoneColor3
    }

    // Outer top edge stones
    var sx = pL + 10f
    while (sx < pR - 10f) {
        val h = hash(sx, pT, 1)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val yOff = hash(sx, pT, 2) * 5f + 3f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(sx, pT + yOff))
        sx += stoneSpacing + hash(sx, pT, 3) * 8f
    }

    // Outer bottom edge stones
    sx = pL + 10f
    while (sx < pR - 10f) {
        val h = hash(sx, pB, 4)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val yOff = hash(sx, pB, 5) * 5f + 3f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(sx, pB - yOff))
        sx += stoneSpacing + hash(sx, pB, 6) * 8f
    }

    // Outer left edge stones
    var sy = pT + 10f
    while (sy < pB - 10f) {
        val h = hash(pL, sy, 7)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val xOff = hash(pL, sy, 8) * 5f + 3f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(pL + xOff, sy))
        sy += stoneSpacing + hash(pL, sy, 9) * 8f
    }

    // Outer right edge stones
    sy = pT + 10f
    while (sy < pB - 10f) {
        val h = hash(pR, sy, 10)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val xOff = hash(pR, sy, 11) * 5f + 3f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(pR - xOff, sy))
        sy += stoneSpacing + hash(pR, sy, 12) * 8f
    }

    // Inner top edge stones (along grid top)
    sx = gL + 5f
    while (sx < gR - 5f) {
        val h = hash(sx, gT, 13)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val yOff = hash(sx, gT, 14) * 4f + 2f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(sx, gT - yOff))
        sx += stoneSpacing + hash(sx, gT, 15) * 8f
    }

    // Inner bottom edge stones (along grid bottom)
    sx = gL + 5f
    while (sx < gR - 5f) {
        val h = hash(sx, gB, 16)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val yOff = hash(sx, gB, 17) * 4f + 2f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(sx, gB + yOff))
        sx += stoneSpacing + hash(sx, gB, 18) * 8f
    }

    // Inner left edge stones (along grid left)
    sy = gT + 5f
    while (sy < gB - 5f) {
        val h = hash(gL, sy, 19)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val xOff = hash(gL, sy, 20) * 4f + 2f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(gL - xOff, sy))
        sy += stoneSpacing + hash(gL, sy, 21) * 8f
    }

    // Inner right edge stones (along grid right)
    sy = gT + 5f
    while (sy < gB - 5f) {
        val h = hash(gR, sy, 22)
        val r = minStoneRadius + h * (maxStoneRadius - minStoneRadius)
        val xOff = hash(gR, sy, 23) * 4f + 2f
        drawCircle(color = stoneColor(h), radius = r, center = Offset(gR + xOff, sy))
        sy += stoneSpacing + hash(gR, sy, 24) * 8f
    }
}
