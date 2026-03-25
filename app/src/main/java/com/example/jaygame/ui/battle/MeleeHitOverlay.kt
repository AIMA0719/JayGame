package com.example.jaygame.ui.battle

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
import com.example.jaygame.R
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.*

private const val SLASH_DURATION_MS = 400L

// 종족별 슬래시 글로우 색상 (family 0~5)
private val SlashGlowColors = arrayOf(
    Color(0xFFFFCC80).copy(alpha = 0.45f),  // 0: HUMAN — warm gold
    Color(0xFF64B5F6).copy(alpha = 0.45f),  // 1: SPIRIT — frost blue
    Color(0xFF81C784).copy(alpha = 0.45f),  // 2: ANIMAL — nature green
    Color(0xFFFF9800).copy(alpha = 0.45f),  // 3: ROBOT — flame orange
    Color(0xFFEF5350).copy(alpha = 0.45f),  // 4: DEMON — blood red
    Color(0xFFCE93D8).copy(alpha = 0.45f),  // 5: fallback — purple
)

// 종족별 슬래시 틴트 (ColorFilter로 스프라이트에 적용)
private val SlashTintColors = arrayOf(
    Color(0xFFFFD700),  // HUMAN — gold
    Color(0xFF88DDFF),  // SPIRIT — ice blue
    Color(0xFF66FF66),  // ANIMAL — green
    Color(0xFFFF6B35),  // ROBOT — orange
    Color(0xFFFF4444),  // DEMON — red
    Color(0xFFBB86FC),  // fallback — purple
)

@Composable
fun MeleeHitOverlay() {
    val events by BattleBridge.meleeHitEvents.collectAsState()
    val context = LocalContext.current

    val slashBitmap = remember { decodeScaledBitmap(context, R.drawable.proj_melee_slash, 128)!! }
    val critBitmap = remember { decodeAssetBitmap(context, "fx/fx_crit.png") }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = System.currentTimeMillis()
        val w = size.width
        val h = size.height

        for (event in events) {
            val age = now - event.timestamp
            if (age > SLASH_DURATION_MS || age < 0) continue

            val progress = age.toFloat() / SLASH_DURATION_MS
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val scale = 0.8f + progress * 0.5f

            val x = event.x * w
            val y = event.y * h
            val family = event.family.coerceIn(0, SlashGlowColors.size - 1)

            // 크기: 75px 기본 (화면 비례), 크리티컬 1.5배
            val baseSize = 75f * (w / 720f) * scale
            val critMult = if (event.isCrit) 1.5f else 1f
            val slashSize = baseSize * critMult
            val halfSize = slashSize / 2f

            val angleDeg = event.angle * (180f / PI.toFloat())

            // 종족별 글로우
            drawCircle(
                color = SlashGlowColors[family],
                radius = slashSize * 0.7f,
                center = Offset(x, y),
                alpha = alpha,
                blendMode = BlendMode.Screen,
            )

            // 슬래시 스프라이트 (종족별 틴트)
            rotate(degrees = angleDeg, pivot = Offset(x, y)) {
                drawImage(
                    image = slashBitmap,
                    dstOffset = IntOffset((x - halfSize).toInt(), (y - halfSize).toInt()),
                    dstSize = IntSize(slashSize.toInt(), slashSize.toInt()),
                    alpha = alpha,
                    colorFilter = ColorFilter.tint(
                        SlashTintColors[family],
                        blendMode = BlendMode.Modulate,
                    ),
                )
            }

            // 크리티컬 히트 스프라이트
            if (event.isCrit && critBitmap != null) {
                val critScale = 0.6f + progress * 0.6f
                val critSize = (slashSize * 1.3f * critScale).toInt()
                val critHalf = critSize / 2f
                drawImage(
                    image = critBitmap,
                    dstOffset = IntOffset((x - critHalf).toInt(), (y - critHalf).toInt()),
                    dstSize = IntSize(critSize, critSize),
                    alpha = alpha * 0.85f,
                )
            }
        }
    }
}
