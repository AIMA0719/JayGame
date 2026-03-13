package com.example.jaygame.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Pre-allocated Color constants (avoid GC)
private val GradeColors = GradeColorsByIndex
private val GradeNames = GradeNamesByIndex
private val MergeGhostColor = Color(0xFFAADDFF)
private val MergeBurstWhite = Color(0xFFFFFFFF)
private val MergeBurstCyan = Color(0xFF80DEEA)
private val MergeRingColor = Color(0xFFB2EBF2)
private val GoldenFlashColor = Color(0xFFFFD700)
private val GoldenFlashBright = Color(0xFFFFF8DC)
private val GoldenBurstCore = Color(0xFFFFE082)
private val GoldenBurstOuter = Color(0xFFFFAB00)
private val GoldenSparkle = Color(0xFFFFF176)
private val LuckyTextColor = Color(0xFFFFD700)
private val LuckyTextShadow = android.graphics.Color.argb(200, 0x8B, 0x6B, 0x00)

// Pre-allocated Paint to avoid per-frame allocation in Canvas draw
private val LuckyTextPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(255, 0xFF, 0xD7, 0x00)
    textSize = 48f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    setShadowLayer(8f, 0f, 2f, LuckyTextShadow)
}

/**
 * Shows merge result with visual effects:
 * - D2: Two ghost units fly toward center, burst effect at merge point
 * - D3: Lucky merge gets golden full-screen flash + "LUCKY!" text
 * - Shows result unit name + grade info
 * Auto-dismiss after ~2.5 seconds.
 */
@Composable
fun MergeEffectOverlay() {
    val effect by BattleBridge.mergeEffect.collectAsState()
    val data = effect ?: return

    val unitDef = UNIT_DEFS_MAP[data.resultUnitId]
    val grade = if (data.resultUnitId >= 0) com.example.jaygame.data.unitGradeOf(data.resultUnitId) else -1
    val gradeColor = GradeColors.getOrElse(grade) { Color.White }
    val gradeName = GradeNames.getOrElse(grade) { "" }

    // D2: Ghost unit fly-in animation (0 -> 1 over 400ms)
    val flyProgress = remember { Animatable(0f) }
    // D2: Burst animation at center (0 -> 1 over 500ms)
    val burstProgress = remember { Animatable(0f) }
    // D3: Golden flash for lucky merge (0 -> peak -> 0)
    val goldenFlashAlpha = remember { Animatable(0f) }
    // D3: "LUCKY!" text scale
    val luckyScale = remember { Animatable(0f) }
    // D3: "LUCKY!" text alpha
    val luckyAlpha = remember { Animatable(0f) }
    // Card reveal
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(data) {
        // Phase 1: Ghost units fly in (D2)
        flyProgress.snapTo(0f)
        burstProgress.snapTo(0f)
        cardAlpha.snapTo(0f)

        flyProgress.animateTo(
            1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
        )

        // Phase 2: Burst at center (D2)
        launch {
            burstProgress.animateTo(
                1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
            )
        }

        // Phase 2b: If lucky, golden flash (D3)
        if (data.isLucky) {
            launch {
                goldenFlashAlpha.snapTo(0f)
                goldenFlashAlpha.animateTo(0.5f, animationSpec = tween(150))
                goldenFlashAlpha.animateTo(0.15f, animationSpec = tween(200))
                goldenFlashAlpha.animateTo(0.35f, animationSpec = tween(100))
                goldenFlashAlpha.animateTo(0f, animationSpec = tween(400))
            }
            launch {
                luckyScale.snapTo(0f)
                luckyAlpha.snapTo(0f)
                luckyAlpha.animateTo(1f, animationSpec = tween(150))
                luckyScale.animateTo(
                    1.2f,
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 300f),
                )
                delay(600)
                luckyAlpha.animateTo(0f, animationSpec = tween(300))
            }
        }

        // Phase 3: Show info card
        delay(200)
        cardAlpha.animateTo(1f, animationSpec = tween(300))

        // Auto-dismiss
        delay(if (data.isLucky) 2000L else 1300L)
        BattleBridge.clearMergeEffect()
    }

    val scale by animateFloatAsState(
        targetValue = if (data.isLucky) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "mergeScale",
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // D3: Full-screen golden flash for lucky merge
        val flashAlpha = goldenFlashAlpha.value
        if (flashAlpha > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Golden radial gradient flash
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldenFlashBright.copy(alpha = flashAlpha),
                            GoldenFlashColor.copy(alpha = flashAlpha * 0.6f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.8f,
                    ),
                    size = size,
                )
            }
        }

        // D2: Ghost unit fly-in + burst Canvas
        val fly = flyProgress.value
        val burst = burstProgress.value
        if (fly > 0.01f || burst > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // D2: Two ghost units flying from left and right toward center
                if (fly < 1f) {
                    val ghostAlpha = (1f - fly).coerceIn(0f, 0.8f)
                    val ghostScale = 1f - fly * 0.5f

                    // Left ghost: starts at ~30% x, flies to center
                    val leftX = cx - (cx * 0.35f) * (1f - fly)
                    val leftY = cy - 10f * sin(fly * PI.toFloat() * 2f)
                    val ghostRadius = 18f * ghostScale

                    // Ghost body (left)
                    drawCircle(
                        color = MergeGhostColor.copy(alpha = ghostAlpha * 0.7f),
                        radius = ghostRadius,
                        center = Offset(leftX, leftY),
                    )
                    drawCircle(
                        color = MergeBurstWhite.copy(alpha = ghostAlpha * 0.4f),
                        radius = ghostRadius * 1.4f,
                        center = Offset(leftX, leftY),
                    )

                    // Right ghost: starts at ~70% x, flies to center
                    val rightX = cx + (cx * 0.35f) * (1f - fly)
                    val rightY = cy + 10f * sin(fly * PI.toFloat() * 2f)

                    drawCircle(
                        color = MergeGhostColor.copy(alpha = ghostAlpha * 0.7f),
                        radius = ghostRadius,
                        center = Offset(rightX, rightY),
                    )
                    drawCircle(
                        color = MergeBurstWhite.copy(alpha = ghostAlpha * 0.4f),
                        radius = ghostRadius * 1.4f,
                        center = Offset(rightX, rightY),
                    )
                }

                // D2: Burst effect at center after ghosts converge
                if (burst > 0.01f) {
                    val burstAlpha = (1f - burst).coerceIn(0f, 1f)
                    val burstRadius = 20f + burst * 80f

                    // Core glow
                    drawCircle(
                        color = MergeBurstWhite.copy(alpha = burstAlpha * 0.8f),
                        radius = burstRadius * 0.3f,
                        center = Offset(cx, cy),
                    )
                    // Mid ring
                    drawCircle(
                        color = MergeBurstCyan.copy(alpha = burstAlpha * 0.5f),
                        radius = burstRadius * 0.6f,
                        center = Offset(cx, cy),
                    )
                    // Outer ring
                    drawCircle(
                        color = MergeRingColor.copy(alpha = burstAlpha * 0.25f),
                        radius = burstRadius,
                        center = Offset(cx, cy),
                    )

                    // Radial particle lines
                    val lineCount = 8
                    for (i in 0 until lineCount) {
                        val angle = (i.toFloat() / lineCount) * 2f * PI.toFloat()
                        val innerR = burstRadius * 0.2f
                        val outerR = burstRadius * (0.5f + burst * 0.5f)
                        val sx = cx + cos(angle) * innerR
                        val sy = cy + sin(angle) * innerR
                        val ex = cx + cos(angle) * outerR
                        val ey = cy + sin(angle) * outerR
                        drawLine(
                            color = MergeBurstWhite.copy(alpha = burstAlpha * 0.6f),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 2f,
                        )
                    }

                    // D3: Golden burst overlay for lucky merge
                    if (data.isLucky && burst > 0.05f) {
                        val goldenAlpha = burstAlpha * 0.7f
                        drawCircle(
                            color = GoldenBurstCore.copy(alpha = goldenAlpha * 0.6f),
                            radius = burstRadius * 0.5f,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            color = GoldenBurstOuter.copy(alpha = goldenAlpha * 0.3f),
                            radius = burstRadius * 1.2f,
                            center = Offset(cx, cy),
                        )
                        // Golden sparkle particles
                        val sparkCount = 12
                        for (i in 0 until sparkCount) {
                            val angle = (i.toFloat() / sparkCount) * 2f * PI.toFloat() + burst * 2f
                            val dist = burstRadius * (0.4f + burst * 0.8f)
                            val sparkX = cx + cos(angle) * dist
                            val sparkY = cy + sin(angle) * dist
                            val sparkSize = 3f * burstAlpha
                            drawCircle(
                                color = GoldenSparkle.copy(alpha = burstAlpha * 0.9f),
                                radius = sparkSize,
                                center = Offset(sparkX, sparkY),
                            )
                        }
                    }
                }
            }
        }

        // D3: "LUCKY!" text at center with scale-up animation
        if (data.isLucky && luckyAlpha.value > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val textScale = luckyScale.value.coerceAtLeast(0.01f)
                val textAlpha = luckyAlpha.value
                val cx = size.width / 2f
                val cy = size.height / 2f - 80f

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.scale(textScale, textScale, cx, cy)
                // Reuse pre-allocated Paint, only update alpha per frame
                LuckyTextPaint.alpha = (textAlpha * 255).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    "LUCKY!",
                    cx,
                    cy,
                    LuckyTextPaint,
                )
                drawContext.canvas.nativeCanvas.restore()
            }
        }

        // Info card (existing UI with fade-in)
        val cAlpha = cardAlpha.value
        if (cAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = cAlpha,
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                DarkNavy.copy(alpha = 0.95f),
                                gradeColor.copy(alpha = 0.3f),
                            )
                        )
                    )
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
                        text = "\u2726 조합 성공!",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Unit icon
                if (unitDef != null) {
                    Image(
                        painter = painterResource(id = unitDef.iconRes),
                        contentDescription = unitDef.name,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Grade + unit name
                Text(
                    text = "$gradeName ${unitDef?.name ?: ""}",
                    color = gradeColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
