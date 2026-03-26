package com.example.jaygame.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.jaygame.R
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.components.blueprintDisplayName
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin


// Rainbow colors for legend+ summon burst
private val RainbowColors = arrayOf(
    Color(0xFFFF4444), // red
    Color(0xFFFF8800), // orange
    Color(0xFFFFDD00), // yellow
    Color(0xFF44DD44), // green
    Color(0xFF4488FF), // blue
    Color(0xFF8844FF), // indigo
    Color(0xFFFF44FF), // violet
)

// Pre-allocated colors with unique values
private val DustColor = Color(0xFFFFE0B2)
private val DiamondColor = Color(0xFFBBDEFB)
private val LensFlareColor = Color(0xFFFFFFCC)

// Pre-allocated Stroke objects
private val RingStroke1_5f = Stroke(width = 1.5f)
private val RingStroke2f = Stroke(width = 2f)
private val RingStroke2_5f = Stroke(width = 2.5f)
private val RingStroke3f = Stroke(width = 3f)
private val RingStroke4f = Stroke(width = 4f)

// Two-pi constant
private const val TWO_PI = 6.2831853f

@Composable
fun SummonEffectOverlay() {
    val summonResult by BattleBridge.summonResult.collectAsState()
    val data = summonResult ?: return

    val context = LocalContext.current
    val summonBitmap = remember { decodeScaledBitmap(context, R.drawable.vfx_summon, 128)!! }

    val unitDef = UNIT_DEFS_MAP[data.unitDefId]
    val grade = data.grade

    // Auto-dismiss: longer for higher grades
    LaunchedEffect(data) {
        val duration = when {
            grade >= 4 -> 2500L
            grade >= 3 -> 2000L
            else -> 1200L
        }
        delay(duration)
        BattleBridge.clearSummonResult()
    }

    val gradeColor = GradeColorsByIndex.getOrElse(grade) { Color.White }
    val gradeName = GradeNamesByIndex.getOrElse(grade) { "" }
    val summonText = remember(gradeName) { "$gradeName 소환 성공!" }
    val isHeroPlus = grade >= 2
    val isLegendPlus = grade >= 3

    // Card scale: starts small, springs to 1.0
    val cardScaleTarget = remember { mutableFloatStateOf(0.6f) }
    LaunchedEffect(data) { cardScaleTarget.floatValue = 1f }
    val scale by animateFloatAsState(
        targetValue = cardScaleTarget.floatValue,
        animationSpec = spring(
            dampingRatio = if (isLegendPlus) 0.25f else 0.35f,
            stiffness = if (isLegendPlus) 200f else 250f,
        ),
        label = "summonScale",
    )

    // Card slide-up: starts offset below, springs to 0
    val cardSlideTarget = remember { mutableFloatStateOf(60f) }
    LaunchedEffect(data) { cardSlideTarget.floatValue = 0f }
    val slideUp by animateFloatAsState(
        targetValue = cardSlideTarget.floatValue,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f,
        ),
        label = "summonSlideUp",
    )

    // Card rotation: starts tilted, springs to 0
    val cardRotTarget = remember { mutableFloatStateOf(if (isLegendPlus) -8f else -4f) }
    LaunchedEffect(data) { cardRotTarget.floatValue = 0f }
    val rotation by animateFloatAsState(
        targetValue = cardRotTarget.floatValue,
        animationSpec = spring(
            dampingRatio = if (isLegendPlus) 0.4f else 0.5f,
            stiffness = 200f,
        ),
        label = "summonRotation",
    )

    // Burst expand animation
    val burstDuration = when {
        grade >= 4 -> 1200
        grade >= 3 -> 1000
        grade >= 2 -> 800
        grade >= 1 -> 700
        else -> 600
    }
    val burstProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = burstDuration),
        label = "burstExpand",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "summonFx")
    val fxTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "summonFxTime",
    )

    // Icon bounce
    val iconBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconBounce",
    )

    // Rainbow hue rotation for legend+
    val rainbowShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainbowShift",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Background dim
        val dimAlpha = when {
            grade >= 4 -> 0.6f
            grade >= 3 -> 0.4f
            grade >= 2 -> 0.2f
            grade >= 1 -> 0.1f
            else -> 0.05f
        }
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }

        // Canvas VFX
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val bp = burstProgress
            val maxR = size.minDimension * 0.4f * bp
            val fadeAlpha = (1f - bp).coerceIn(0f, 1f)

            // ── Summon sprite (grade 2+) ──
            if (grade >= 2 && bp > 0.01f) {
                val summonAlpha = (1f - bp * 0.6f).coerceIn(0f, 1f)
                val summonSize = (maxR * 1.8f).toInt().coerceAtLeast(1)
                val summonHalf = summonSize / 2f
                drawImage(
                    image = summonBitmap,
                    dstOffset = IntOffset(
                        (cx - summonHalf).toInt(),
                        (cy - summonHalf).toInt(),
                    ),
                    dstSize = IntSize(summonSize, summonSize),
                    alpha = summonAlpha * 0.7f,
                    blendMode = BlendMode.Screen,
                )
            }

            // ══════════════════════════════════════════════
            // LIGHT PILLAR (all grades)
            // ══════════════════════════════════════════════
            val pillarWidth = when (grade) {
                0 -> 4f
                1 -> 6f
                2 -> 8f
                3 -> 12f
                else -> 16f
            }
            val pillarHeightRatio = when (grade) {
                0 -> 0.3f
                1 -> 0.4f
                2 -> 0.5f
                3 -> 0.8f
                else -> 1.0f
            }
            val pillarAlpha = fadeAlpha * when (grade) {
                0 -> 0.3f
                1 -> 0.4f
                2 -> 0.5f
                3 -> 0.7f
                else -> 0.85f
            }
            if (pillarAlpha > 0.01f) {
                val pillarH = size.height * pillarHeightRatio * bp
                // White core
                drawLine(
                    color = Color.White.copy(alpha = pillarAlpha * 0.8f),
                    start = Offset(cx, cy - pillarH / 2f),
                    end = Offset(cx, cy + pillarH / 2f),
                    strokeWidth = pillarWidth * 0.5f,
                    cap = StrokeCap.Round,
                )
                // Grade-color glow (wider)
                drawLine(
                    color = gradeColor.copy(alpha = pillarAlpha * 0.4f),
                    start = Offset(cx, cy - pillarH / 2f),
                    end = Offset(cx, cy + pillarH / 2f),
                    strokeWidth = pillarWidth * 2f,
                    cap = StrokeCap.Round,
                )
                // Outer soft glow
                drawLine(
                    color = gradeColor.copy(alpha = pillarAlpha * 0.15f),
                    start = Offset(cx, cy - pillarH / 2f),
                    end = Offset(cx, cy + pillarH / 2f),
                    strokeWidth = pillarWidth * 5f,
                    cap = StrokeCap.Round,
                )

                // Mythic+: horizontal cross pillar
                if (grade >= 4) {
                    val crossW = size.width * 0.6f * bp
                    drawLine(
                        color = Color.White.copy(alpha = pillarAlpha * 0.5f),
                        start = Offset(cx - crossW / 2f, cy),
                        end = Offset(cx + crossW / 2f, cy),
                        strokeWidth = pillarWidth * 0.4f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = gradeColor.copy(alpha = pillarAlpha * 0.25f),
                        start = Offset(cx - crossW / 2f, cy),
                        end = Offset(cx + crossW / 2f, cy),
                        strokeWidth = pillarWidth * 1.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            when {
                // ══════════════════════════════════════════════
                // GRADE 0 (Common) - "Spark Burst"
                // ══════════════════════════════════════════════
                grade == 0 -> {
                    // Center flash (white circle expanding and fading)
                    val flashR = maxR * 0.4f
                    val flashAlpha = (1f - bp * 1.5f).coerceIn(0f, 0.7f)
                    if (flashAlpha > 0.01f) {
                        drawCircle(
                            color = Color.White.copy(alpha = flashAlpha),
                            radius = flashR,
                            center = Offset(cx, cy),
                        )
                    }

                    // 8 short spark lines shooting outward
                    val sparkFade = (1f - bp * 1.2f).coerceIn(0f, 0.6f)
                    for (i in 0 until 8) {
                        val angle = (i / 8f) * TWO_PI
                        val innerR = maxR * 0.15f * bp
                        val outerR = maxR * 0.5f * bp
                        drawLine(
                            color = gradeColor.copy(alpha = sparkFade),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Single expanding ring
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ringAlpha),
                        radius = maxR * 0.7f,
                        center = Offset(cx, cy),
                        style = RingStroke1_5f,
                    )

                    // 6 dust particles floating upward
                    for (i in 0 until 6) {
                        val angle = (i / 6f) * TWO_PI + 0.5f
                        val dist = maxR * 0.15f + maxR * 0.3f * bp
                        val yOff = -maxR * 0.3f * bp // float upward
                        val dustAlpha = (1f - bp * 1.1f).coerceIn(0f, 0.4f)
                        drawCircle(
                            color = DustColor.copy(alpha = dustAlpha),
                            radius = 2f,
                            center = Offset(
                                cx + cos(angle) * dist,
                                cy + sin(angle) * dist + yOff,
                            ),
                        )
                    }
                }

                // ══════════════════════════════════════════════
                // GRADE 1 (Rare) - "Crystal Shatter"
                // ══════════════════════════════════════════════
                grade == 1 -> {
                    // Center flash (brighter than Common)
                    val flashR = maxR * 0.45f
                    val flashAlpha = (1f - bp * 1.3f).coerceIn(0f, 0.8f)
                    if (flashAlpha > 0.01f) {
                        drawCircle(
                            color = Color.White.copy(alpha = flashAlpha),
                            radius = flashR,
                            center = Offset(cx, cy),
                        )
                    }

                    // 12 burst lines with alternating long/short
                    val lineFade = (1f - bp * 1.0f).coerceIn(0f, 0.6f)
                    for (i in 0 until 12) {
                        val angle = (i / 12f) * TWO_PI
                        val isLong = i % 2 == 0
                        val innerR = maxR * 0.1f * bp
                        val lenMult = if (isLong) 0.65f else 0.4f
                        val outerR = maxR * lenMult * bp
                        drawLine(
                            color = gradeColor.copy(alpha = lineFade),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Ring 1 - immediate
                    val ring1Alpha = (1f - bp).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ring1Alpha),
                        radius = maxR * 0.6f,
                        center = Offset(cx, cy),
                        style = RingStroke2f,
                    )

                    // Ring 2 - staggered (starts at 50% progress)
                    val ring2Progress = ((bp - 0.5f) * 2f).coerceIn(0f, 1f)
                    if (ring2Progress > 0.01f) {
                        val ring2Alpha = (1f - ring2Progress).coerceIn(0f, 0.4f)
                        drawCircle(
                            color = gradeColor.copy(alpha = ring2Alpha),
                            radius = maxR * 0.8f * ring2Progress,
                            center = Offset(cx, cy),
                            style = RingStroke1_5f,
                        )
                    }

                    // 6 diamond shapes (rotated squares) scattering outward
                    for (i in 0 until 6) {
                        val angle = (i / 6f) * TWO_PI + 0.3f
                        val dist = maxR * 0.2f + maxR * 0.5f * bp
                        val diamondAlpha = (1f - bp * 1.1f).coerceIn(0f, 0.5f)
                        val dx = cx + cos(angle) * dist
                        val dy = cy + sin(angle) * dist
                        val diamondSize = 4f * (1f - bp * 0.5f)
                        // Draw diamond as 4 lines (rotated 45deg square)
                        drawLine(
                            color = DiamondColor.copy(alpha = diamondAlpha),
                            start = Offset(dx, dy - diamondSize),
                            end = Offset(dx + diamondSize, dy),
                            strokeWidth = 1.5f,
                        )
                        drawLine(
                            color = DiamondColor.copy(alpha = diamondAlpha),
                            start = Offset(dx + diamondSize, dy),
                            end = Offset(dx, dy + diamondSize),
                            strokeWidth = 1.5f,
                        )
                        drawLine(
                            color = DiamondColor.copy(alpha = diamondAlpha),
                            start = Offset(dx, dy + diamondSize),
                            end = Offset(dx - diamondSize, dy),
                            strokeWidth = 1.5f,
                        )
                        drawLine(
                            color = DiamondColor.copy(alpha = diamondAlpha),
                            start = Offset(dx - diamondSize, dy),
                            end = Offset(dx, dy - diamondSize),
                            strokeWidth = 1.5f,
                        )
                    }
                }

                // ══════════════════════════════════════════════
                // GRADE 2 (Hero) - "Energy Vortex"
                // ══════════════════════════════════════════════
                grade == 2 -> {
                    // Pulsing center glow
                    val pulseR = maxR * 0.2f * (1f + sin(fxTime * TWO_PI) * 0.3f)
                    val pulseAlpha = 0.4f * (1f - bp * 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = pulseAlpha),
                        radius = pulseR,
                        center = Offset(cx, cy),
                    )

                    // 16 burst lines with spiral motion
                    val lineFade = (1f - bp * 0.8f).coerceIn(0f, 0.6f)
                    val spiralSpin = fxTime * TWO_PI * 2f
                    for (i in 0 until 16) {
                        val baseAngle = (i / 16f) * TWO_PI
                        val angle = baseAngle + spiralSpin * 0.3f // lines rotate as they expand
                        val innerR = maxR * 0.1f
                        val outerR = maxR * 0.6f * (0.6f + sin(fxTime * 6f + i) * 0.4f)
                        drawLine(
                            color = gradeColor.copy(alpha = lineFade * 0.6f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Counter-rotating dot ring 1 (clockwise)
                    val dotRingR1 = maxR * 0.65f
                    for (i in 0 until 10) {
                        val angle = (i / 10f) * TWO_PI + fxTime * TWO_PI * 1.5f
                        val dotAlpha = 0.5f * (1f - bp * 0.4f)
                        drawCircle(
                            color = gradeColor.copy(alpha = dotAlpha),
                            radius = 3f,
                            center = Offset(cx + cos(angle) * dotRingR1, cy + sin(angle) * dotRingR1),
                        )
                    }

                    // Counter-rotating dot ring 2 (counter-clockwise)
                    val dotRingR2 = maxR * 0.45f
                    for (i in 0 until 8) {
                        val angle = (i / 8f) * TWO_PI - fxTime * TWO_PI * 1.2f
                        val dotAlpha = 0.4f * (1f - bp * 0.4f)
                        drawCircle(
                            color = Color.White.copy(alpha = dotAlpha),
                            radius = 2.5f,
                            center = Offset(cx + cos(angle) * dotRingR2, cy + sin(angle) * dotRingR2),
                        )
                    }

                    // Expanding rings
                    val ring1Alpha = (1f - bp).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ring1Alpha),
                        radius = maxR * 0.7f,
                        center = Offset(cx, cy),
                        style = RingStroke2_5f,
                    )
                    val ring2Progress = ((bp - 0.3f) * 1.43f).coerceIn(0f, 1f)
                    if (ring2Progress > 0.01f) {
                        drawCircle(
                            color = gradeColor.copy(alpha = (1f - ring2Progress) * 0.35f),
                            radius = maxR * 0.9f * ring2Progress,
                            center = Offset(cx, cy),
                            style = RingStroke2f,
                        )
                    }

                    // Edge vignette glow (grade-colored)
                    val vignetteAlpha = 0.15f * (1f - bp * 0.7f)
                    // Top edge
                    drawRect(
                        color = gradeColor.copy(alpha = vignetteAlpha),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height * 0.15f),
                    )
                    // Bottom edge
                    drawRect(
                        color = gradeColor.copy(alpha = vignetteAlpha),
                        topLeft = Offset(0f, size.height * 0.85f),
                        size = Size(size.width, size.height * 0.15f),
                    )
                }

                // ══════════════════════════════════════════════
                // GRADE 3 (Legend) - "Rainbow Ascension"
                // ══════════════════════════════════════════════
                grade == 3 -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.7f)

                    // Screen flash at start
                    if (bp < 0.3f) {
                        val flashAlpha = (0.3f - bp) / 0.3f * 0.3f
                        drawRect(
                            color = gradeColor.copy(alpha = flashAlpha),
                            size = size,
                        )
                    }

                    // 3 staggered expanding rings
                    for (r in 0 until 3) {
                        val ringDelay = r * 0.1f // 100ms apart approx
                        val ringBp = ((bp - ringDelay) / (1f - ringDelay)).coerceIn(0f, 1f)
                        if (ringBp > 0.01f) {
                            val ringR = maxR * (0.3f + ringBp * 0.7f)
                            val rAlpha = (1f - ringBp) * 0.4f
                            drawCircle(
                                color = gradeColor.copy(alpha = rAlpha),
                                radius = ringR,
                                center = Offset(cx, cy),
                                style = RingStroke3f,
                            )
                        }
                    }

                    // 20 rainbow burst lines with spiral
                    val spiralSpin = fxTime * TWO_PI * 1.5f
                    for (i in 0 until 20) {
                        val baseAngle = (i / 20f) * TWO_PI
                        val angle = baseAngle + spiralSpin * 0.2f
                        val innerR = maxR * 0.1f
                        val outerR = innerR + maxR * 0.8f * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                        val colorIdx = ((i + rainbowShift.toInt()) % RainbowColors.size)
                        drawLine(
                            color = RainbowColors[colorIdx].copy(alpha = ringAlpha * 0.6f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Orbiting rainbow particles in expanding spiral
                    val particleCount = 16
                    for (i in 0 until particleCount) {
                        val t = (fxTime + i * 0.0625f) % 1f
                        val spiralR = maxR * (0.2f + t * 0.7f)
                        val spiralAngle = t * TWO_PI * 3f + (i / particleCount.toFloat()) * TWO_PI
                        val pAlpha = (1f - t) * 0.7f
                        val pColor = RainbowColors[(i + rainbowShift.toInt()) % RainbowColors.size]
                        drawCircle(
                            color = pColor.copy(alpha = pAlpha),
                            radius = 3.5f,
                            center = Offset(
                                cx + cos(spiralAngle) * spiralR,
                                cy + sin(spiralAngle) * spiralR,
                            ),
                        )
                    }

                    // Light rays (pulsing/breathing)
                    val rayCount = 10
                    for (i in 0 until rayCount) {
                        val angle = (i.toFloat() / rayCount) * TWO_PI + fxTime * 1.5f
                        val breathe = 0.7f + sin(fxTime * TWO_PI * 2f + i) * 0.3f
                        val rayLen = size.maxDimension * 0.5f * bp * breathe
                        val rayAlpha = (1f - bp * 0.5f) * 0.15f
                        drawLine(
                            color = gradeColor.copy(alpha = rayAlpha),
                            start = Offset(cx, cy),
                            end = Offset(cx + cos(angle) * rayLen, cy + sin(angle) * rayLen),
                            strokeWidth = 6f + sin(fxTime * 6f + i) * 4f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Horizontal lens flare
                    val flareAlpha = fadeAlpha * 0.25f
                    if (flareAlpha > 0.01f) {
                        val flareLen = size.width * 0.7f * bp
                        drawLine(
                            color = LensFlareColor.copy(alpha = flareAlpha),
                            start = Offset(cx - flareLen / 2f, cy),
                            end = Offset(cx + flareLen / 2f, cy),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                        // Wider soft glow
                        drawLine(
                            color = LensFlareColor.copy(alpha = flareAlpha * 0.3f),
                            start = Offset(cx - flareLen / 2f, cy),
                            end = Offset(cx + flareLen / 2f, cy),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // ══════════════════════════════════════════════
                // GRADE 4+ (Mythic/Immortal) - "Divine Revelation"
                // ══════════════════════════════════════════════
                else -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.8f)

                    // Screen flash (brighter and longer)
                    if (bp < 0.35f) {
                        val flashAlpha = (0.35f - bp) / 0.35f * 0.5f
                        drawRect(
                            color = Color.White.copy(alpha = flashAlpha),
                            size = size,
                        )
                    }

                    // 4 staggered expanding rings
                    for (r in 0 until 4) {
                        val ringDelay = r * 0.08f
                        val ringBp = ((bp - ringDelay) / (1f - ringDelay)).coerceIn(0f, 1f)
                        if (ringBp > 0.01f) {
                            val ringR = maxR * (0.3f + ringBp * 0.7f)
                            val rAlpha = (1f - ringBp) * 0.45f
                            drawCircle(
                                color = gradeColor.copy(alpha = rAlpha),
                                radius = ringR,
                                center = Offset(cx, cy),
                                style = RingStroke4f,
                            )
                        }
                    }

                    // 24 rainbow burst lines with spiral (thicker)
                    val spiralSpin = fxTime * TWO_PI * 1.5f
                    for (i in 0 until 24) {
                        val baseAngle = (i / 24f) * TWO_PI
                        val angle = baseAngle + spiralSpin * 0.25f
                        val innerR = maxR * 0.08f
                        val outerR = innerR + maxR * 0.9f * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                        val colorIdx = ((i + rainbowShift.toInt()) % RainbowColors.size)
                        drawLine(
                            color = RainbowColors[colorIdx].copy(alpha = ringAlpha * 0.65f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Orbiting particles in figure-8 pattern
                    val particleCount = 20
                    for (i in 0 until particleCount) {
                        val t = (fxTime + i * 0.05f) % 1f
                        val fig8Angle = t * TWO_PI * 2f + (i / particleCount.toFloat()) * TWO_PI
                        // Figure-8 (lemniscate)
                        val fig8R = maxR * 0.5f
                        val px = cx + cos(fig8Angle) * fig8R
                        val py = cy + sin(fig8Angle * 2f) * fig8R * 0.4f
                        val pAlpha = (0.5f + sin(t * TWO_PI) * 0.3f) * (1f - bp * 0.3f)
                        val pColor = RainbowColors[(i + rainbowShift.toInt()) % RainbowColors.size]
                        drawCircle(
                            color = pColor.copy(alpha = pAlpha),
                            radius = 5f,
                            center = Offset(px, py),
                        )
                    }

                    // Full screen light rays (pulsing, rainbow)
                    val rayCount = 14
                    for (i in 0 until rayCount) {
                        val angle = (i.toFloat() / rayCount) * TWO_PI + fxTime * 1.5f
                        val breathe = 0.7f + sin(fxTime * TWO_PI * 2f + i) * 0.3f
                        val rayLen = size.maxDimension * 0.8f * bp * breathe
                        val rayAlpha = (1f - bp * 0.5f) * 0.25f
                        val rayColor = RainbowColors[(i + rainbowShift.toInt()) % RainbowColors.size]
                        drawLine(
                            color = rayColor.copy(alpha = rayAlpha),
                            start = Offset(cx, cy),
                            end = Offset(cx + cos(angle) * rayLen, cy + sin(angle) * rayLen),
                            strokeWidth = 6f + sin(fxTime * 6f + i) * 4f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Pulsing 4-pointed star at center (rotating)
                    val starSize = maxR * 0.15f * (1f + sin(fxTime * TWO_PI * 3f) * 0.3f)
                    val starAlpha = 0.7f * (1f - bp * 0.4f)
                    val starRot = fxTime * TWO_PI * 0.5f
                    // 4 points of the star
                    for (p in 0 until 4) {
                        val sAngle = (p / 4f) * TWO_PI + starRot
                        drawLine(
                            color = Color.White.copy(alpha = starAlpha),
                            start = Offset(cx, cy),
                            end = Offset(cx + cos(sAngle) * starSize, cy + sin(sAngle) * starSize),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Inner glow of star
                    drawCircle(
                        color = Color.White.copy(alpha = starAlpha * 0.5f),
                        radius = starSize * 0.3f,
                        center = Offset(cx, cy),
                    )

                    // Horizontal lens flare (wider than Legend)
                    val flareAlpha = fadeAlpha * 0.35f
                    if (flareAlpha > 0.01f) {
                        val flareLen = size.width * 0.9f * bp
                        drawLine(
                            color = LensFlareColor.copy(alpha = flareAlpha),
                            start = Offset(cx - flareLen / 2f, cy),
                            end = Offset(cx + flareLen / 2f, cy),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = LensFlareColor.copy(alpha = flareAlpha * 0.3f),
                            start = Offset(cx - flareLen / 2f, cy),
                            end = Offset(cx + flareLen / 2f, cy),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Edge vignette with grade color
                    val vignetteAlpha = 0.2f * (1f - bp * 0.5f)
                    drawRect(
                        color = gradeColor.copy(alpha = vignetteAlpha),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height * 0.15f),
                    )
                    drawRect(
                        color = gradeColor.copy(alpha = vignetteAlpha),
                        topLeft = Offset(0f, size.height * 0.85f),
                        size = Size(size.width, size.height * 0.15f),
                    )
                }
            }
        }

        // ══════════════════════════════════════════════
        // Summon result card with slide-up + rotation + spring
        // ══════════════════════════════════════════════
        val cardFontSize = when {
            grade >= 4 -> 22.sp
            grade >= 3 -> 20.sp
            else -> 16.sp
        }
        val cardIconSize = when {
            grade >= 4 -> 80.dp
            grade >= 3 -> 72.dp
            else -> 56.dp
        }

        val cardBgBrush = remember(gradeColor, isLegendPlus) {
            Brush.verticalGradient(
                colors = listOf(
                    DarkNavy.copy(alpha = 0.95f),
                    gradeColor.copy(alpha = if (isLegendPlus) 0.5f else 0.3f),
                )
            )
        }

        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    translationY = slideUp + 40f * (1f - scale) // slide up from below
                }
                .clip(RoundedCornerShape(16.dp))
                .background(cardBgBrush)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLegendPlus) {
                Text(
                    text = if (grade >= 4) "★ LEGENDARY ★" else "Lucky",
                    color = if (grade >= 4) Color.White else Gold,
                    fontSize = if (grade >= 4) 22.sp else 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (unitDef != null) {
                // Icon with bounce effect
                val bounceScale = if (isLegendPlus) 0.15f else 0.1f
                val bounceOffset = (iconBounce - 0.5f) * (if (isLegendPlus) 10f else 6f)
                Image(
                    painter = painterResource(id = unitDef.iconRes),
                    contentDescription = unitDef.name,
                    modifier = Modifier
                        .size(cardIconSize)
                        .graphicsLayer {
                            translationY = bounceOffset
                            scaleX = 1f + (iconBounce - 0.5f) * bounceScale
                            scaleY = 1f + (iconBounce - 0.5f) * bounceScale
                        },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = summonText,
                color = gradeColor,
                fontSize = cardFontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            // Unit name
            val unitName = unitDef?.name
                ?: if (data.blueprintId.isNotEmpty()) blueprintDisplayName(data.blueprintId) else null
            if (unitName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = unitName,
                    color = Color.White,
                    fontSize = if (isLegendPlus) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
