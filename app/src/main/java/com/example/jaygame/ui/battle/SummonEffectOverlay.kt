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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.ui.components.blueprintDisplayName
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val SummonGradeColors = GradeColorsByIndex
private val SummonGradeNames = GradeNamesByIndex

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

@Composable
fun SummonEffectOverlay() {
    val summonResult by BattleBridge.summonResult.collectAsState()
    val data = summonResult ?: return

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

    val gradeColor = SummonGradeColors.getOrElse(grade) { Color.White }
    val gradeName = SummonGradeNames.getOrElse(grade) { "" }
    val isHeroPlus = grade >= 2
    val isLegendPlus = grade >= 3

    // Card scale with spring bounce — bouncier for higher grades
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = if (isLegendPlus) 0.25f else 0.35f,
            stiffness = if (isLegendPlus) 200f else 250f,
        ),
        label = "summonScale",
    )

    // Card rotation (entrance spin) — more dramatic spin for legend+
    val rotation by animateFloatAsState(
        targetValue = 0f,
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
        // Background dim — intensity scales with grade
        val dimAlpha = when {
            grade >= 4 -> 0.6f  // mythic: very dark
            grade >= 3 -> 0.4f  // legend+
            grade >= 2 -> 0.2f  // hero: slight dim
            else -> 0f          // common/rare: no dim
        }
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }

        // Canvas VFX — grade-differentiated
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val bp = burstProgress
            val maxR = size.minDimension * 0.4f * bp

            when {
                // ── GRADE 0 (Common): minimal sparkle ──
                grade == 0 -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.4f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ringAlpha * 0.2f),
                        radius = maxR * 0.6f,
                        center = Offset(cx, cy),
                    )
                    // Just 4 small lines
                    for (i in 0 until 4) {
                        val angle = (i / 4f) * 6.283f + fxTime * 1.5f
                        val innerR = maxR * 0.2f
                        val outerR = innerR + maxR * 0.3f * bp
                        drawLine(
                            color = gradeColor.copy(alpha = ringAlpha * 0.5f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // ── GRADE 1 (Rare): basic burst ──
                grade == 1 -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ringAlpha * 0.25f),
                        radius = maxR,
                        center = Offset(cx, cy),
                    )
                    for (i in 0 until 8) {
                        val angle = (i / 8f) * 6.283f + fxTime * 2f
                        val innerR = maxR * 0.15f
                        val outerR = innerR + maxR * 0.5f * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                        drawLine(
                            color = gradeColor.copy(alpha = ringAlpha * 0.4f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // ── GRADE 2 (Hero): burst + rotating ring ──
                grade == 2 -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.6f)
                    drawCircle(
                        color = gradeColor.copy(alpha = ringAlpha * 0.3f),
                        radius = maxR,
                        center = Offset(cx, cy),
                    )
                    // Burst lines
                    for (i in 0 until 12) {
                        val angle = (i / 12f) * 6.283f + fxTime * 2f
                        val innerR = maxR * 0.15f
                        val outerR = innerR + maxR * 0.6f * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                        drawLine(
                            color = gradeColor.copy(alpha = ringAlpha * 0.5f),
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Rotating ring of dots
                    for (i in 0 until 8) {
                        val angle = (i / 8f) * 6.283f + fxTime * 4f
                        val r = maxR * 0.7f
                        drawCircle(
                            color = gradeColor.copy(alpha = 0.5f * (1f - bp * 0.5f)),
                            radius = 3f,
                            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                        )
                    }
                }

                // ── GRADE 3+ (Legend~Mythic): rainbow burst + expanding rings ──
                else -> {
                    val ringAlpha = (1f - bp).coerceIn(0f, 0.7f)

                    // Multiple expanding glow rings
                    val ringCount = if (grade >= 4) 3 else 2
                    for (r in 0 until ringCount) {
                        val ringPhase = ((fxTime + r * 0.3f) % 1f)
                        val ringR = maxR * (0.3f + ringPhase * 0.7f)
                        val ringA = (1f - ringPhase) * 0.3f
                        drawCircle(
                            color = gradeColor.copy(alpha = ringA),
                            radius = ringR,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (grade >= 4) 4f else 3f,
                            ),
                        )
                    }

                    // Rainbow radial burst lines
                    val lineCount = when {
                        grade <= 3 -> 16
                        else -> 24
                    }
                    val lineLen = maxR * 0.9f
                    val lineWidth = when {
                        grade <= 3 -> 3f
                        else -> 4f
                    }

                    for (i in 0 until lineCount) {
                        val angle = (i.toFloat() / lineCount) * 6.283f + fxTime * 2f
                        val innerR = maxR * 0.1f
                        val outerR = innerR + lineLen * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                        // Rainbow color cycling
                        val colorIdx = ((i + rainbowShift.toInt()) % RainbowColors.size)
                        val lineColor = if (grade >= 3) {
                            RainbowColors[colorIdx].copy(alpha = ringAlpha * 0.6f)
                        } else {
                            gradeColor.copy(alpha = ringAlpha * 0.5f)
                        }
                        drawLine(
                            color = lineColor,
                            start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                            end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                            strokeWidth = lineWidth,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Particle dots — rainbow colored, more for higher grades
                    val particleCount = (grade - 1) * 8
                    for (i in 0 until particleCount) {
                        val angle = (i.toFloat() / particleCount) * 6.283f
                        val r = maxR * (0.2f + ((fxTime + i * 0.07f) % 1f) * 0.8f)
                        val pAlpha = (1f - ((fxTime + i * 0.07f) % 1f)) * 0.7f
                        val wobble = sin(fxTime * 8f + i * 2f) * maxR * 0.05f
                        val pColor = if (grade >= 3) {
                            RainbowColors[(i + rainbowShift.toInt()) % RainbowColors.size]
                        } else {
                            gradeColor
                        }
                        drawCircle(
                            color = pColor.copy(alpha = pAlpha),
                            radius = if (grade >= 4) 5f else 3.5f,
                            center = Offset(cx + cos(angle) * r + wobble, cy + sin(angle) * r),
                        )
                    }

                    // Full screen light rays for grade 3+ (Legend/Mythic)
                    if (grade >= 3) {
                        val rayCount = if (grade >= 4) 14 else 10
                        for (i in 0 until rayCount) {
                            val angle = (i.toFloat() / rayCount) * 6.283f + fxTime * 1.5f
                            val rayLen = size.maxDimension * (if (grade >= 4) 0.8f else 0.5f) * bp
                            val rayAlpha = (1f - bp * 0.5f) * (if (grade >= 4) 0.25f else 0.15f)
                            val rayColor = if (grade >= 4) {
                                RainbowColors[(i + rainbowShift.toInt()) % RainbowColors.size]
                            } else {
                                gradeColor
                            }
                            drawLine(
                                color = rayColor.copy(alpha = rayAlpha),
                                start = Offset(cx, cy),
                                end = Offset(cx + cos(angle) * rayLen, cy + sin(angle) * rayLen),
                                strokeWidth = 6f + sin(fxTime * 6f + i) * 4f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    // Screen flash — brighter for higher grades
                    if (bp < 0.3f && grade >= 3) {
                        val flashIntensity = when {
                            grade >= 4 -> 0.5f   // mythic
                            else -> 0.3f          // legend
                        }
                        val flashAlpha = (0.3f - bp) / 0.3f * flashIntensity
                        val flashColor = if (grade >= 4) Color.White else gradeColor
                        drawRect(
                            color = flashColor.copy(alpha = flashAlpha),
                            size = size,
                        )
                    }
                }
            }
        }

        // Summon result card with rotation + spring
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

        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkNavy.copy(alpha = 0.95f),
                            gradeColor.copy(alpha = if (isLegendPlus) 0.5f else 0.3f),
                        )
                    )
                )
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
                // Icon with bounce effect — bigger bounce for higher grades
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
                text = "$gradeName 소환 성공!",
                color = gradeColor,
                fontSize = cardFontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            // Unit name — legacy or blueprint
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
