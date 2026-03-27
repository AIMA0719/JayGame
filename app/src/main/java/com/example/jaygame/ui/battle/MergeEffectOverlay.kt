package com.example.jaygame.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.ui.components.blueprintDisplayName
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ── Pre-allocated Color constants (GC policy: no allocation in draw lambdas) ──
private val GradeColors = GradeColorsByIndex
private val GradeNames = GradeNamesByIndex

private const val TWO_PI = 6.2831853f

private val GoldenColor = Color(0xFFFFD700)          // flash + cross lines
private val GoldenFlashBright = Color(0xFFFFF8DC)
private val GoldenBurstCore = Color(0xFFFFE082)
private val GoldenBurstOuter = Color(0xFFFFAB00)
private val GoldenSparkle = Color(0xFFFFF176)

private val LuckyTextShadow = android.graphics.Color.argb(200, 0x8B, 0x6B, 0x00)

// Pre-allocated Paint objects for native canvas text rendering.
// These are mutated per frame (.alpha), which is safe because Compose Canvas
// drawing for this overlay runs single-threaded on the main/render thread.
private val LuckyGlowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(160, 0xFF, 0xD7, 0x00)
    textSize = 56f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    maskFilter = android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.NORMAL)
}

private val LuckyTextPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(255, 0xFF, 0xFF, 0xFF)
    textSize = 48f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    setShadowLayer(4f, 0f, 2f, LuckyTextShadow)
}

/**
 * Merge result overlay with enhanced visual effects:
 * - 3 ghost units converge with comet trails
 * - Staggered shockwave rings
 * - Impact flash + zoom punch
 * - 12 tapered radial burst lines with rotation
 * - Lucky: golden shockwave, cross pattern, spiral sparkles, outlined text, double-pulse flash
 * - Card slides up with spring bounce
 * Auto-dismiss: 1500ms normal, 2500ms lucky
 */
@Composable
fun MergeEffectOverlay() {
    val effect by BattleBridge.mergeEffect.collectAsState()
    val data = effect ?: return

    val bp = if (data.resultBlueprintId.isNotEmpty() && BlueprintRegistry.isReady)
        BlueprintRegistry.instance.findById(data.resultBlueprintId) else null
    val grade = bp?.grade?.ordinal ?: -1
    val gradeColor = GradeColors.getOrElse(grade) { Color.White }
    val gradeName = GradeNames.getOrElse(grade) { "" }

    // ── Animatables ──
    // Ghost fly-in (0 -> 1 over 400ms)
    val flyProgress = remember { Animatable(0f) }
    // Impact flash (0 -> 1 over 100ms)
    val impactFlash = remember { Animatable(0f) }
    // 3 staggered shockwave rings
    val shockwave1 = remember { Animatable(0f) }
    val shockwave2 = remember { Animatable(0f) }
    val shockwave3 = remember { Animatable(0f) }
    // Radial burst lines (0 -> 1 over 500ms)
    val burstProgress = remember { Animatable(0f) }
    // Zoom punch (1.0 -> 1.03 -> 1.0)
    val zoomPunch = remember { Animatable(1f) }
    // Card reveal: alpha + translateY + scale
    val cardAlpha = remember { Animatable(0f) }
    val cardTranslateY = remember { Animatable(50f) }
    val cardScale = remember { Animatable(0.9f) }
    // Lucky-specific
    val goldenFlashAlpha = remember { Animatable(0f) }
    val luckyScale = remember { Animatable(0f) }
    val luckyAlpha = remember { Animatable(0f) }
    val goldenShockwave = remember { Animatable(0f) }
    val goldenCrossAlpha = remember { Animatable(0f) }

    LaunchedEffect(data) {
        // Reset all
        flyProgress.snapTo(0f)
        impactFlash.snapTo(0f)
        shockwave1.snapTo(0f)
        shockwave2.snapTo(0f)
        shockwave3.snapTo(0f)
        burstProgress.snapTo(0f)
        zoomPunch.snapTo(1f)
        cardAlpha.snapTo(0f)
        cardTranslateY.snapTo(50f)
        cardScale.snapTo(0.9f)
        goldenFlashAlpha.snapTo(0f)
        luckyScale.snapTo(0f)
        luckyAlpha.snapTo(0f)
        goldenShockwave.snapTo(0f)
        goldenCrossAlpha.snapTo(0f)

        // Phase 1: 3 ghosts fly in (0-400ms)
        flyProgress.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))

        // Phase 2: At 400ms - impact flash + zoom punch + shockwaves + burst
        launch {
            impactFlash.animateTo(1f, animationSpec = tween(100))
        }
        launch {
            zoomPunch.animateTo(1.03f, animationSpec = tween(80))
            zoomPunch.animateTo(1f, animationSpec = tween(120))
        }
        // Staggered shockwaves: 0ms, 100ms, 200ms delay
        launch {
            shockwave1.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100)
            shockwave2.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            shockwave3.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        // Radial burst lines (400-900ms)
        launch {
            burstProgress.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }

        // Lucky overlay effects
        if (data.isLucky) {
            // Golden flash double-pulse
            launch {
                goldenFlashAlpha.animateTo(0.55f, animationSpec = tween(100))
                goldenFlashAlpha.animateTo(0.12f, animationSpec = tween(150))
                goldenFlashAlpha.animateTo(0.45f, animationSpec = tween(100))
                goldenFlashAlpha.animateTo(0f, animationSpec = tween(450))
            }
            // Golden shockwave
            launch {
                goldenShockwave.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
            }
            // Golden cross pattern
            launch {
                goldenCrossAlpha.animateTo(0.9f, animationSpec = tween(150))
                delay(300)
                goldenCrossAlpha.animateTo(0f, animationSpec = tween(350))
            }
            // LUCKY! text
            launch {
                luckyAlpha.animateTo(1f, animationSpec = tween(150))
                luckyScale.animateTo(1.2f, animationSpec = spring(dampingRatio = 0.35f, stiffness = 300f))
                delay(800)
                luckyAlpha.animateTo(0f, animationSpec = tween(300))
            }
        }

        // Phase 3: Card slides up at 500ms (after ghosts + slight delay)
        delay(100) // 100ms after convergence
        launch {
            cardAlpha.animateTo(1f, animationSpec = tween(300))
        }
        launch {
            cardTranslateY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            cardScale.animateTo(1.05f, animationSpec = tween(200))
            cardScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f))
        }

        // Auto-dismiss
        delay(if (data.isLucky) 2500L else 1500L)
        BattleBridge.clearMergeEffect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = zoomPunch.value,
                scaleY = zoomPunch.value,
            )
    ) {

        // ── Lucky: Full-screen golden flash (double-pulse) ──
        val flashA = goldenFlashAlpha.value
        if (flashA > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Concentric circles instead of Brush.radialGradient (no Brush allocation)
                drawCircle(
                    color = GoldenFlashBright.copy(alpha = flashA),
                    radius = size.width * 0.3f,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = GoldenColor.copy(alpha = flashA * 0.6f),
                    radius = size.width * 0.6f,
                    center = Offset(cx, cy),
                )
            }
        }

        // ── Main effects Canvas ──
        val fly = flyProgress.value
        val burst = burstProgress.value
        val impact = impactFlash.value
        val sw1 = shockwave1.value
        val sw2 = shockwave2.value
        val sw3 = shockwave3.value
        val gSw = goldenShockwave.value
        val gCross = goldenCrossAlpha.value

        val hasGhosts = fly > 0.01f && fly < 1f
        val hasEffects = impact > 0.01f || sw1 > 0.01f || sw2 > 0.01f || sw3 > 0.01f ||
                burst > 0.01f || (data.isLucky && (gSw > 0.01f || gCross > 0.01f))

        if (hasGhosts || hasEffects) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = min(size.width, size.height) * 0.45f

                // ── 3 Ghost units with comet trails (loop, no array allocation) ──
                if (hasGhosts) {
                    val ghostAlpha = (1f - fly).coerceIn(0f, 0.85f)
                    val ghostScale = 1f - fly * 0.7f // 1.0 -> 0.3
                    val ghostRadius = 16f * ghostScale

                    // Draw trails + bodies for all 3 ghosts
                    val trailSteps = 4
                    for (g in 0 until 3) {
                        val startX = when (g) { 0 -> cx * 0.15f; 1 -> size.width - cx * 0.15f; else -> cx }
                        val startY = when (g) { 0, 1 -> cy * 0.25f; else -> size.height - cy * 0.15f }

                        // Comet trails
                        for (t in trailSteps downTo 1) {
                            val trailFly = (fly - t * 0.06f).coerceIn(0f, 1f)
                            val trailAlpha = ghostAlpha * (1f - t * 0.22f)
                            val trailRadius = ghostRadius * (1f - t * 0.18f)
                            if (trailAlpha > 0.01f && trailRadius > 0.5f) {
                                val tx = startX + (cx - startX) * trailFly
                                val ty = startY + (cy - startY) * trailFly
                                drawCircle(
                                    color = gradeColor.copy(alpha = trailAlpha * 0.4f),
                                    radius = trailRadius,
                                    center = Offset(tx, ty),
                                )
                            }
                        }

                        // Ghost body (glow + core)
                        val gx = startX + (cx - startX) * fly
                        val gy = startY + (cy - startY) * fly
                        drawCircle(
                            color = gradeColor.copy(alpha = ghostAlpha * 0.3f),
                            radius = ghostRadius * 1.6f,
                            center = Offset(gx, gy),
                        )
                        drawCircle(
                            color = gradeColor.copy(alpha = ghostAlpha * 0.8f),
                            radius = ghostRadius,
                            center = Offset(gx, gy),
                        )
                    }
                }

                // ── Impact flash (bright white circle at center) ──
                if (impact > 0.01f && impact < 1f) {
                    val flashAlpha = (1f - impact).coerceIn(0f, 1f)
                    val flashRadius = 30f + impact * 20f
                    drawCircle(
                        color = Color.White.copy(alpha = flashAlpha * 0.9f),
                        radius = flashRadius,
                        center = Offset(cx, cy),
                    )
                } else if (impact >= 1f) {
                    // Fade out tail: use burst progress for continued fade
                    val tailAlpha = (1f - burst * 2f).coerceIn(0f, 0.5f)
                    if (tailAlpha > 0.01f) {
                        drawCircle(
                            color = Color.White.copy(alpha = tailAlpha),
                            radius = 40f,
                            center = Offset(cx, cy),
                        )
                    }
                }

                // ── Staggered shockwave rings ──
                // Note: Stroke() allocates per frame, but only 3-4 instances during a brief
                // ~300ms effect window. Acceptable trade-off vs. complexity of alternatives.
                // Ring 1: white (inner)
                if (sw1 > 0.01f) {
                    val r1 = sw1 * maxR * 0.7f
                    val a1 = (0.8f * (1f - sw1)).coerceIn(0f, 0.8f)
                    drawCircle(
                        color = Color.White.copy(alpha = a1),
                        radius = r1,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3f - sw1 * 2f),
                    )
                }
                // Ring 2: grade color (mid)
                if (sw2 > 0.01f) {
                    val r2 = sw2 * maxR * 0.85f
                    val a2 = (0.7f * (1f - sw2)).coerceIn(0f, 0.7f)
                    drawCircle(
                        color = gradeColor.copy(alpha = a2),
                        radius = r2,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f - sw2 * 1.5f),
                    )
                }
                // Ring 3: grade color faded (outer)
                if (sw3 > 0.01f) {
                    val r3 = sw3 * maxR
                    val a3 = (0.5f * (1f - sw3)).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = a3 * 0.6f),
                        radius = r3,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f - sw3),
                    )
                }

                // ── 12 Radial burst lines with taper + rotation ──
                if (burst > 0.01f) {
                    val burstAlpha = (1f - burst).coerceIn(0f, 1f)
                    val lineCount = 12
                    val angularOffset = burst * 0.3f // slight rotation during expansion
                    for (i in 0 until lineCount) {
                        val baseAngle = (i.toFloat() / lineCount) * TWO_PI
                        val angle = baseAngle + angularOffset
                        // Varying lengths: alternate long/short
                        val lengthMul = if (i % 2 == 0) 1f else 0.7f
                        val innerR = maxR * 0.05f
                        val outerR = maxR * (0.3f + burst * 0.5f) * lengthMul
                        val sx = cx + cos(angle) * innerR
                        val sy = cy + sin(angle) * innerR
                        val ex = cx + cos(angle) * outerR
                        val ey = cy + sin(angle) * outerR
                        // Tapered: thick at center, thin at edge
                        val strokeW = (3f * burstAlpha * (1f - burst * 0.6f)).coerceAtLeast(0.5f)
                        drawLine(
                            color = Color.White.copy(alpha = burstAlpha * 0.7f),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // ── Lucky: Golden shockwave ──
                if (data.isLucky && gSw > 0.01f) {
                    val gR = gSw * maxR * 1.1f
                    val gA = (0.6f * (1f - gSw)).coerceIn(0f, 0.6f)
                    drawCircle(
                        color = GoldenBurstCore.copy(alpha = gA),
                        radius = gR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4f - gSw * 3f),
                    )
                    drawCircle(
                        color = GoldenBurstOuter.copy(alpha = gA * 0.5f),
                        radius = gR * 0.7f,
                        center = Offset(cx, cy),
                    )
                }

                // ── Lucky: Star/cross pattern (4 long golden lines in + shape) ──
                if (data.isLucky && gCross > 0.01f) {
                    val crossLen = maxR * 0.9f
                    val crossStroke = 3f * gCross
                    // Horizontal
                    drawLine(
                        color = GoldenColor.copy(alpha = gCross * 0.8f),
                        start = Offset(cx - crossLen, cy),
                        end = Offset(cx + crossLen, cy),
                        strokeWidth = crossStroke,
                        cap = StrokeCap.Round,
                    )
                    // Vertical
                    drawLine(
                        color = GoldenColor.copy(alpha = gCross * 0.8f),
                        start = Offset(cx, cy - crossLen),
                        end = Offset(cx, cy + crossLen),
                        strokeWidth = crossStroke,
                        cap = StrokeCap.Round,
                    )
                }

                // ── Lucky: 16 golden sparkle particles with spiral orbit ──
                if (data.isLucky && gSw > 0.01f) {
                    val sparkCount = 16
                    val sparkAlpha = (1f - gSw).coerceIn(0f, 1f)
                    for (i in 0 until sparkCount) {
                        val baseAngle = (i.toFloat() / sparkCount) * TWO_PI
                        // Spiral: angle increases with progress, distance grows
                        val spiralAngle = baseAngle + gSw * 3f
                        val dist = maxR * (0.15f + gSw * 0.7f)
                        val sparkX = cx + cos(spiralAngle) * dist
                        val sparkY = cy + sin(spiralAngle) * dist
                        val sparkSize = 3f * sparkAlpha * (0.5f + 0.5f * sin(baseAngle * 3f + gSw * 5f))
                        if (sparkSize > 0.3f) {
                            drawCircle(
                                color = GoldenSparkle.copy(alpha = sparkAlpha * 0.85f),
                                radius = sparkSize,
                                center = Offset(sparkX, sparkY),
                            )
                        }
                    }
                }
            }
        }

        // ── Lucky: "LUCKY!" text with glow outline ──
        if (data.isLucky && luckyAlpha.value > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val textScale = luckyScale.value.coerceAtLeast(0.01f)
                val textAlpha = luckyAlpha.value
                val tcx = size.width / 2f
                val tcy = size.height / 2f - 80f

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.scale(textScale, textScale, tcx, tcy)

                // Glow layer (larger, blurred golden)
                LuckyGlowPaint.alpha = (textAlpha * 160).toInt()
                drawContext.canvas.nativeCanvas.drawText("LUCKY!", tcx, tcy, LuckyGlowPaint)

                // Sharp white text on top
                LuckyTextPaint.alpha = (textAlpha * 255).toInt()
                drawContext.canvas.nativeCanvas.drawText("LUCKY!", tcx, tcy, LuckyTextPaint)

                drawContext.canvas.nativeCanvas.restore()
            }
        }

        // ── Result info card (slide up + scale bounce) ──
        val cardBgBrush = remember(gradeColor) {
            Brush.verticalGradient(
                colors = listOf(
                    DarkNavy.copy(alpha = 0.95f),
                    gradeColor.copy(alpha = 0.3f),
                )
            )
        }

        val cAlpha = cardAlpha.value
        if (cAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = cardScale.value,
                        scaleY = cardScale.value,
                        alpha = cAlpha,
                        translationY = cardTranslateY.value,
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBgBrush)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Lucky header
                if (data.isLucky) {
                    Text(
                        text = "\u2605 JACKPOT! \u2605",
                        color = Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text(
                        text = "\u2726 \uc870\ud569 \uc131\uacf5!",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Unit icon
                if (bp != null) {
                    val iconRes = com.example.jaygame.ui.screens.blueprintIconRes(bp)
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = bp.name,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Grade + unit name (blueprint)
                val unitName = bp?.name
                    ?: if (data.resultBlueprintId.isNotEmpty()) blueprintDisplayName(data.resultBlueprintId) else null
                Text(
                    text = "$gradeName ${unitName ?: ""}",
                    color = gradeColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
