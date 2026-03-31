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
import com.example.jaygame.bridge.BattleBridge
import kotlin.math.*

private const val SLASH_DURATION_MS = 400L

private val SlashTintColors = arrayOf(
    Color(0xFFFFD700),  // HUMAN — gold
    Color(0xFF88DDFF),  // SPIRIT — ice blue
    Color(0xFF66FF66),  // ANIMAL — green
    Color(0xFFFF6B35),  // ROBOT — orange
    Color(0xFFFF4444),  // DEMON — red
    Color(0xFFBB86FC),  // fallback — purple
)

private val SlashAnimMap = arrayOf(
    "anim_sfx3a_1",  // 0: HUMAN
    "anim_sfx3a_3",  // 1: SPIRIT
    "anim_sfx3a_2",  // 2: ANIMAL
    "anim_sfx3a_4",  // 3: ROBOT
    "anim_sfx3d_2",  // 4: DEMON
    "anim_sfx3a_1",  // 5: fallback
)

private const val CRIT_ANIM = "anim_sfx3b_1"

@Composable
fun MeleeHitOverlay() {
    val events by BattleBridge.meleeHitEvents.collectAsState()
    val context = LocalContext.current

    val manifest = remember { loadAnimManifest(context) }

    // LRU 캐시 — 슬래시 시트는 6종+1(크릿)만이라 전부 로드해도 작음
    val sheetCache = remember {
        mutableMapOf<String, ImageBitmap?>().apply {
            for (name in SlashAnimMap.toSet() + CRIT_ANIM) {
                put(name, decodeAssetBitmap(context, "fx_anim/$name.png"))
            }
        }
    }

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
            val family = event.family.coerceIn(0, SlashAnimMap.size - 1)

            val baseSize = 110f * (w / 720f) * scale
            val critMult = if (event.isCrit) 1.5f else 1f
            val slashSize = baseSize * critMult
            val halfSize = slashSize / 2f

            val angleDeg = event.angle * (180f / PI.toFloat())

            // 종족별 슬래시 애니메이션 시트
            val animName = SlashAnimMap[family]
            val info = manifest[animName]
            val sheet = sheetCache[animName]

            if (info != null && sheet != null && info.frames > 1) {
                val elapsed = age / 1000f
                val frameDur = SLASH_DURATION_MS / 1000f / info.frames
                val frameIndex = (elapsed / frameDur).toInt().coerceIn(0, info.frames - 1)
                val srcX = frameIndex * info.cellW

                rotate(degrees = angleDeg, pivot = Offset(x, y)) {
                    drawImage(
                        image = sheet,
                        srcOffset = IntOffset(srcX, 0),
                        srcSize = IntSize(info.cellW, info.cellH),
                        dstOffset = IntOffset((x - halfSize).toInt(), (y - halfSize).toInt()),
                        dstSize = IntSize(slashSize.toInt(), slashSize.toInt()),
                        alpha = alpha,
                        colorFilter = ColorFilter.tint(
                            SlashTintColors[family],
                            blendMode = BlendMode.Modulate,
                        ),
                    )
                }
            }

            // 크리티컬 히트 — 콤보 슬래시 오버레이
            if (event.isCrit) {
                val critInfo = manifest[CRIT_ANIM]
                val critSheet = sheetCache[CRIT_ANIM]
                if (critInfo != null && critSheet != null && critInfo.frames > 1) {
                    val elapsed = age / 1000f
                    val frameDur = SLASH_DURATION_MS / 1000f / critInfo.frames
                    val frameIdx = (elapsed / frameDur).toInt().coerceIn(0, critInfo.frames - 1)
                    val srcX = frameIdx * critInfo.cellW
                    val critScale = 0.6f + progress * 0.6f
                    val critSize = (slashSize * 1.3f * critScale).toInt()
                    val critHalf = critSize / 2f

                    drawImage(
                        image = critSheet,
                        srcOffset = IntOffset(srcX, 0),
                        srcSize = IntSize(critInfo.cellW, critInfo.cellH),
                        dstOffset = IntOffset((x - critHalf).toInt(), (y - critHalf).toInt()),
                        dstSize = IntSize(critSize, critSize),
                        alpha = alpha * 0.85f,
                    )
                }
            }
        }
    }
}
