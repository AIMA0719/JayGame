package com.jay.jaygame.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.jay.jaygame.bridge.BattleBridge
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Pre-allocated colors (GC policy: no allocations inside drawScope) ──
private val GoldColor = Color(0xFFFFD700)               // Gold (text + bars)
private val BossRed = Color(0xFFFF4444)                 // Red (boss text + rays)
private val SubTextColor = Color.White.copy(alpha = 0.8f)
private val ShimmerGold = Color(0xFFFFE57F)

// Special wave colors & labels (indexed by SpecialWaveType ordinal)
private val SpecialWaveColors = arrayOf(
    GoldColor,              // NONE (unused)
    Color(0xFF00E5FF),      // RUSH — 시안
    Color(0xFFFF9100),      // HEAVY_ARMOR — 오렌지
    Color(0xFFAB47BC),      // DARKNESS — 보라
    Color(0xFFFFD740),      // ELITE_MARCH — 노랑
    Color(0xFFFF5252),      // LAST_CHARGE — 빨강
)

private val SpecialWaveLabels = arrayOf(
    "",                          // NONE
    "⚡ 쇄도 — 대량 고속 돌격!",  // RUSH
    "🛡 중장갑 — 느리지만 단단!",  // HEAVY_ARMOR
    "🌑 암흑 — 사거리 감소!",     // DARKNESS
    "⚔ 엘리트 행진 — 전원 엘리트!",// ELITE_MARCH
    "💀 최후의 돌격 — 대규모 급습!",// LAST_CHARGE
)

// Pre-allocated Offsets for Shadow (GC policy)
private val ShadowOffsetLarge = Offset(3f, 3f)
private val ShadowOffsetSmall = Offset(2f, 2f)

/**
 * Shows "WAVE X" announcement when a new wave starts.
 * Normal waves: slide-in from left, scale punch, decorative bars, slide-out right.
 * Boss waves: cinematic treatment with red rays, dramatic shake.
 */
@Composable
fun WaveAnnouncementOverlay() {
    val battleState by BattleBridge.state.collectAsState()
    val currentWave = battleState.currentWave
    val isBoss = battleState.isBossRound
    val gameState = battleState.state // 0=WaveDelay, 1=Playing
    val specialWave = battleState.specialWave // SpecialWaveType ordinal
    val isSpecialWave = !isBoss && specialWave in 1..5

    // Track wave transitions
    val lastAnnouncedWave = remember { mutableIntStateOf(-1) }

    // Animation values
    val slideX = remember { Animatable(-300f) }
    val textScale = remember { Animatable(1f) }
    val textAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(0f) }
    val barProgress = remember { Animatable(0f) }

    // Boss-specific animations
    val bossSubAlpha = remember { Animatable(0f) }
    val bossRayAlpha = remember { Animatable(0f) }

    // Special wave sub-text animation
    val specialSubAlpha = remember { Animatable(0f) }

    // Pre-compute wave text string outside Canvas
    val waveText = remember(currentWave) { "WAVE $currentWave" }

    // Trigger animation when wave changes to Playing state
    LaunchedEffect(currentWave, gameState) {
        if (gameState == 1 && currentWave != lastAnnouncedWave.intValue) {
            lastAnnouncedWave.intValue = currentWave
            coroutineScope {

            // Reset all
            slideX.snapTo(-300f)
            textScale.snapTo(1f)
            textAlpha.snapTo(0f)
            shakeOffset.snapTo(0f)
            barProgress.snapTo(0f)
            bossSubAlpha.snapTo(0f)
            bossRayAlpha.snapTo(0f)
            specialSubAlpha.snapTo(0f)

            if (isBoss) {
                // ── BOSS WAVE TIMELINE ──
                // 0-300ms: text alpha + slide in
                textAlpha.snapTo(1f)
                slideX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))

                // 300ms: scale punch overshoot 1.15 → 1.0
                launch {
                    textScale.snapTo(1.15f)
                    textScale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }

                // 300ms: screen shake (10px, 8 oscillations, 40ms each)
                launch {
                    repeat(8) {
                        shakeOffset.animateTo(10f * (if (it % 2 == 0) 1f else -1f), tween(40))
                    }
                    shakeOffset.animateTo(0f, tween(40))
                }

                // 300ms: bars expand
                launch {
                    barProgress.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }

                // 300ms: boss sub-text fades in (200ms after main text arrives)
                launch {
                    bossSubAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }

                // 300ms: red rays appear and pulse
                launch {
                    bossRayAlpha.animateTo(0.6f, tween(150))
                    repeat(6) {
                        bossRayAlpha.animateTo(0.3f, tween(150))
                        bossRayAlpha.animateTo(0.6f, tween(150))
                    }
                }

                // 620-1500ms: hold
                delay(880)

                // 1500-2000ms: everything fades out
                launch { bossSubAlpha.animateTo(0f, tween(500)) }
                launch { bossRayAlpha.animateTo(0f, tween(300)) }
                launch { barProgress.animateTo(0f, tween(500)) }
                launch {
                    slideX.animateTo(300f, tween(500, easing = FastOutSlowInEasing))
                }
                textAlpha.animateTo(0f, tween(500, easing = LinearEasing))
            } else {
                // ── NORMAL WAVE TIMELINE ──
                // 0-300ms: slide in from left + text appears
                textAlpha.snapTo(1f)
                slideX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))

                // 300ms: scale punch overshoot 1.15 → spring back to 1.0
                launch {
                    textScale.snapTo(1.15f)
                    textScale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }

                // 300ms: bars expand (200ms)
                launch {
                    barProgress.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }

                // Special wave: sub-text fade in + screen shake
                if (isSpecialWave) {
                    launch {
                        specialSubAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                    }
                    launch {
                        repeat(4) {
                            shakeOffset.animateTo(6f * (if (it % 2 == 0) 1f else -1f), tween(50))
                        }
                        shakeOffset.animateTo(0f, tween(50))
                    }
                }

                // Hold at center (longer for special waves)
                delay(if (isSpecialWave) 1200 else 800)

                // Slide out right + fade out
                if (isSpecialWave) {
                    launch { specialSubAlpha.animateTo(0f, tween(400)) }
                }
                launch {
                    barProgress.animateTo(0f, tween(400))
                }
                launch {
                    slideX.animateTo(300f, tween(400, easing = FastOutSlowInEasing))
                }
                textAlpha.animateTo(0f, tween(400, easing = LinearEasing))
            }
            } // coroutineScope
        }
    }

    val textMeasurer = rememberTextMeasurer()

    val anyVisible = textAlpha.value > 0.01f
            || bossRayAlpha.value > 0.01f
            || specialSubAlpha.value > 0.01f

    if (anyVisible) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val alpha = textAlpha.value
            val scale = textScale.value
            val shake = shakeOffset.value
            val slide = slideX.value

            // ── Boss red light rays ──
            val rayAlpha = bossRayAlpha.value
            if (rayAlpha > 0f) {
                val centerX = w / 2f
                val centerY = h * 0.40f
                val rayLength = w * 0.6f
                for (i in 0 until 8) {
                    val angle = i * 45f
                    rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
                        drawLine(
                            color = BossRed.copy(alpha = rayAlpha * 0.4f),
                            start = Offset(centerX, centerY - 30f),
                            end = Offset(centerX, centerY - rayLength),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                        // Wider glow line
                        drawLine(
                            color = BossRed.copy(alpha = rayAlpha * 0.15f),
                            start = Offset(centerX, centerY - 30f),
                            end = Offset(centerX, centerY - rayLength),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            // ── Wave text ──
            val textColor = if (isBoss) BossRed
                else if (isSpecialWave) SpecialWaveColors[specialWave]
                else GoldColor
            val isLateWave = currentWave >= 50
            val fontSize = if (isBoss) 56.sp else 44.sp

            // Gold shimmer for wave 50+: blend gold with brighter gold
            val finalTextColor = if (isLateWave && !isBoss) {
                val shimmerAmount = ((scale - 1f).coerceIn(0f, 0.15f) / 0.15f) * 0.3f
                Color(
                    red = textColor.red + (ShimmerGold.red - textColor.red) * shimmerAmount,
                    green = textColor.green + (ShimmerGold.green - textColor.green) * shimmerAmount,
                    blue = textColor.blue + (ShimmerGold.blue - textColor.blue) * shimmerAmount,
                    alpha = alpha,
                )
            } else {
                textColor.copy(alpha = alpha)
            }

            val style = TextStyle(
                color = finalTextColor,
                fontSize = fontSize * scale,
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f * alpha),
                    offset = ShadowOffsetLarge,
                    blurRadius = 8f,
                ),
            )

            val textLayout = textMeasurer.measure(waveText, style)
            val textX = (w - textLayout.size.width) / 2 + shake + slide
            val textY = h * 0.38f

            drawText(textLayout, topLeft = Offset(textX, textY))

            // ── Decorative bars (glow + solid, expand from center) ──
            val barProg = barProgress.value
            if (barProg > 0.01f) {
                val barW = w * 0.45f * barProg
                val barThick = 3f
                val barGlowThick = 8f
                val barY1 = textY - 14f
                val hasSubText = isBoss || isSpecialWave
                val barY2 = textY + textLayout.size.height + (if (hasSubText) 50f else 10f)
                val barLeft = (w - barW) / 2f

                // Glow layer (wider, lower alpha)
                drawRect(
                    color = GoldColor.copy(alpha = alpha * 0.25f * barProg),
                    topLeft = Offset(barLeft, barY1 - (barGlowThick - barThick) / 2f),
                    size = Size(barW, barGlowThick),
                )
                drawRect(
                    color = GoldColor.copy(alpha = alpha * 0.25f * barProg),
                    topLeft = Offset(barLeft, barY2 - (barGlowThick - barThick) / 2f),
                    size = Size(barW, barGlowThick),
                )

                // Solid layer
                drawRect(
                    color = GoldColor.copy(alpha = alpha * 0.7f * barProg),
                    topLeft = Offset(barLeft, barY1),
                    size = Size(barW, barThick),
                )
                drawRect(
                    color = GoldColor.copy(alpha = alpha * 0.7f * barProg),
                    topLeft = Offset(barLeft, barY2),
                    size = Size(barW, barThick),
                )
            }

            // ── Boss sub-text ──
            val bsAlpha = bossSubAlpha.value
            if (isBoss && bsAlpha > 0.01f) {
                val bossStyle = TextStyle(
                    color = SubTextColor.copy(alpha = bsAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f * bsAlpha),
                        offset = ShadowOffsetSmall,
                        blurRadius = 4f,
                    ),
                )
                val bossLayout = textMeasurer.measure("\u26A0 BOSS WAVE \u26A0", bossStyle)
                val bossX = (w - bossLayout.size.width) / 2 + shake
                val bossY = textY + textLayout.size.height + 14f
                drawText(bossLayout, topLeft = Offset(bossX, bossY))
            }

            // ── Special wave sub-text ──
            val ssAlpha = specialSubAlpha.value
            if (isSpecialWave && ssAlpha > 0.01f) {
                val swColor = SpecialWaveColors[specialWave]
                val specialStyle = TextStyle(
                    color = swColor.copy(alpha = ssAlpha),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f * ssAlpha),
                        offset = ShadowOffsetSmall,
                        blurRadius = 6f,
                    ),
                )
                val specialLabel = SpecialWaveLabels[specialWave]
                val specialLayout = textMeasurer.measure(specialLabel, specialStyle)
                val specialX = (w - specialLayout.size.width) / 2 + shake
                val specialY = textY + textLayout.size.height + 14f
                drawText(specialLayout, topLeft = Offset(specialX, specialY))
            }
        }
    }
}
