package com.jay.jaygame.ui.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jay.jaygame.R
import com.jay.jaygame.bridge.BattleBridge
import kotlin.math.*

// ═══════════════════════════════════════════════════════
// Projectile visual types (race×2 + damageType)
// 0=HUMAN_PHYS(arrow), 1=HUMAN_MAGIC(holy),
// 2=ANIMAL_PHYS(lightning), 3=ANIMAL_MAGIC(poison),
// 4=DEMON_PHYS(fireball), 5=DEMON_MAGIC(dark_orb),
// 6=SPIRIT_PHYS(ice_shard), 7=SPIRIT_MAGIC(magic_orb),
// 8=ROBOT_PHYS(rocket), 9=ROBOT_MAGIC(laser)
// ═══════════════════════════════════════════════════════

private val ProjectileResIds = intArrayOf(
    R.drawable.proj_arrow,       // 0: HUMAN + PHYSICAL
    R.drawable.proj_holy,        // 1: HUMAN + MAGIC
    R.drawable.proj_lightning,   // 2: ANIMAL + PHYSICAL
    R.drawable.proj_poison,      // 3: ANIMAL + MAGIC
    R.drawable.proj_fireball,    // 4: DEMON + PHYSICAL
    R.drawable.proj_dark_orb,    // 5: DEMON + MAGIC
    R.drawable.proj_ice_shard,   // 6: SPIRIT + PHYSICAL
    R.drawable.proj_magic_orb,   // 7: SPIRIT + MAGIC
    R.drawable.proj_rocket,      // 8: ROBOT + PHYSICAL
    R.drawable.proj_laser,       // 9: ROBOT + MAGIC
)

// Pre-allocated glow colors per visual type (avoid per-frame .copy())
private val GlowColors = arrayOf(
    Color(0xFFFFCC80).copy(alpha = 0.35f),  // 0: arrow — warm gold
    Color(0xFFFFF8E1).copy(alpha = 0.35f),  // 1: holy — divine white
    Color(0xFFFFD54F).copy(alpha = 0.35f),  // 2: lightning — electric yellow
    Color(0xFF81C784).copy(alpha = 0.35f),  // 3: poison — toxic green
    Color(0xFFFF6B35).copy(alpha = 0.35f),  // 4: fireball — inferno orange
    Color(0xFF9C27B0).copy(alpha = 0.35f),  // 5: dark orb — shadow purple
    Color(0xFF64B5F6).copy(alpha = 0.35f),  // 6: ice shard — frost blue
    Color(0xFF2196F3).copy(alpha = 0.35f),  // 7: magic orb — arcane blue
    Color(0xFFFF9800).copy(alpha = 0.35f),  // 8: rocket — flame orange
    Color(0xFFEF5350).copy(alpha = 0.35f),  // 9: laser — hot red
)

// Trail color (dimmer glow for trail particles)
private val TrailColors = arrayOf(
    Color(0xFFFFCC80).copy(alpha = 0.18f),
    Color(0xFFFFF8E1).copy(alpha = 0.18f),
    Color(0xFFFFD54F).copy(alpha = 0.18f),
    Color(0xFF81C784).copy(alpha = 0.18f),
    Color(0xFFFF6B35).copy(alpha = 0.18f),
    Color(0xFF9C27B0).copy(alpha = 0.18f),
    Color(0xFF64B5F6).copy(alpha = 0.18f),
    Color(0xFF2196F3).copy(alpha = 0.18f),
    Color(0xFFFF9800).copy(alpha = 0.18f),
    Color(0xFFEF5350).copy(alpha = 0.18f),
)

// Default image angle offsets (degrees) — most images point upper-right (-45°)
private val ImageBaseAngles = floatArrayOf(
    -45f,   // 0: arrow
    -45f,   // 1: holy
    -45f,   // 2: lightning
    -45f,   // 3: poison
    -45f,   // 4: fireball
    -45f,   // 5: dark_orb
    -45f,   // 6: ice_shard
    -45f,   // 7: magic_orb
    -45f,   // 8: rocket
    0f,     // 9: laser (horizontal)
)

private const val VISUAL_TYPE_COUNT = 10

@Composable
fun ProjectileOverlay() {
    val projData by BattleBridge.projectiles.collectAsState()
    val context = LocalContext.current

    // Load all projectile bitmaps once (pre-scaled to 128×128 for performance)
    val bitmaps = remember {
        ProjectileResIds.map { resId -> decodeScaledBitmap(context, resId, 128) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "projFx")
    val fxTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fxTime",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val data = projData
        val w = size.width
        val h = size.height
        val count = data.count
        // LOD: skip trails/glow when projectile count is high
        val drawExtras = count < 200

        for (i in 0 until count) {
            val srcX = data.srcXs[i] * w
            val srcY = data.srcYs[i] * h
            val curX = data.dstXs[i] * w
            val curY = data.dstYs[i] * h
            val visualType = data.types[i].coerceIn(0, VISUAL_TYPE_COUNT - 1)
            val grade = if (i < data.grades.size) data.grades[i] else 0

            val bitmap = bitmaps[visualType]

            // Direction angle (degrees, screen coords: Y-down)
            val angleDeg = atan2(curY - srcY, curX - srcX) * (180f / PI.toFloat())
            val rotation = angleDeg - ImageBaseAngles[visualType]

            // Size scales with grade: 54 → 66 → 78 → 90 → 102 → 114
            val baseSize = (54f + grade * 12f) * (w / 720f)
            val pulse = 1f + sin(fxTime * 2f * PI.toFloat() + i * 0.7f) * 0.06f
            val projSize = baseSize * pulse
            val halfSize = projSize / 2f

            // ── Projectile image with rotation & additive blending ──
            rotate(degrees = rotation, pivot = Offset(curX, curY)) {
                if (bitmap != null) {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = IntOffset(
                            (curX - halfSize).toInt(),
                            (curY - halfSize).toInt(),
                        ),
                        dstSize = IntSize(projSize.toInt(), projSize.toInt()),
                        blendMode = BlendMode.Screen,
                    )
                } else {
                    drawCircle(
                        color = GlowColors[visualType],
                        radius = halfSize * 0.7f,
                        center = Offset(curX, curY),
                    )
                }
            }
        }
    }
}
