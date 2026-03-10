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
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val SummonGradeColors = GradeColorsByIndex
private val SummonGradeNames = GradeNamesByIndex

@Composable
fun SummonEffectOverlay() {
    val summonResult by BattleBridge.summonResult.collectAsState()
    val data = summonResult ?: return

    val unitDef = UNIT_DEFS_MAP[data.unitDefId]

    // Auto-dismiss
    LaunchedEffect(data) {
        delay(1500)
        BattleBridge.clearSummonResult()
    }

    val gradeColor = SummonGradeColors.getOrElse(data.grade) { Color.White }
    val gradeName = SummonGradeNames.getOrElse(data.grade) { "" }
    val isRare = data.grade >= 3

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "summonScale",
    )

    // Burst expand animation
    val burstProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Background dim for rare summons
        if (isRare) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
        }

        // Canvas aura burst effect
        if (data.grade >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val bp = burstProgress
                val maxR = size.minDimension * 0.4f * bp
                val grade = data.grade

                // Outer glow ring
                val ringAlpha = (1f - bp).coerceIn(0f, 0.6f)
                drawCircle(
                    color = gradeColor.copy(alpha = ringAlpha * 0.3f),
                    radius = maxR,
                    center = Offset(cx, cy),
                )

                // Radial burst lines
                val lineCount = when {
                    grade <= 1 -> 6
                    grade <= 3 -> 12
                    grade <= 5 -> 18
                    else -> 24
                }
                val lineLen = maxR * when {
                    grade <= 1 -> 0.5f
                    grade <= 3 -> 0.7f
                    else -> 0.9f
                }
                val lineWidth = when {
                    grade <= 2 -> 2f
                    grade <= 4 -> 3f
                    else -> 4f
                }

                for (i in 0 until lineCount) {
                    val angle = (i.toFloat() / lineCount) * 2f * Math.PI.toFloat() + fxTime * 2f
                    val innerR = maxR * 0.15f
                    val outerR = innerR + lineLen * (0.5f + sin(fxTime * 6f + i) * 0.5f)
                    val alpha2 = ringAlpha * (0.4f + sin(fxTime * 4f + i * 1.3f) * 0.3f)
                    drawLine(
                        color = gradeColor.copy(alpha = alpha2),
                        start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                        end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round,
                    )
                }

                // Particle dots for grade 3+
                if (grade >= 3) {
                    val particleCount = (grade - 2) * 6
                    for (i in 0 until particleCount) {
                        val angle = (i.toFloat() / particleCount) * 2f * Math.PI.toFloat()
                        val r = maxR * (0.3f + ((fxTime + i * 0.1f) % 1f) * 0.7f)
                        val pAlpha = (1f - ((fxTime + i * 0.1f) % 1f)) * 0.6f
                        val wobble = sin(fxTime * 8f + i * 2f) * maxR * 0.05f
                        drawCircle(
                            color = gradeColor.copy(alpha = pAlpha),
                            radius = if (grade >= 5) 4f else 3f,
                            center = Offset(cx + cos(angle) * r + wobble, cy + sin(angle) * r),
                        )
                    }
                }

                // Full screen flash for grade 5+
                if (grade >= 5 && bp < 0.3f) {
                    val flashAlpha = (0.3f - bp) / 0.3f * 0.4f
                    drawRect(
                        color = gradeColor.copy(alpha = flashAlpha),
                        size = size,
                    )
                }
            }
        }

        // Summon result card
        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkNavy.copy(alpha = 0.95f),
                            gradeColor.copy(alpha = 0.3f),
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isRare) {
                Text(
                    text = "Lucky",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (unitDef != null) {
                Image(
                    painter = painterResource(id = unitDef.iconRes),
                    contentDescription = unitDef.name,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "$gradeName 소환 성공!",
                color = gradeColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            if (unitDef != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = unitDef.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
