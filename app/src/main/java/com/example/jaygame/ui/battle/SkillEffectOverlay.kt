package com.example.jaygame.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.SkillVfxType
import kotlin.math.sin

// SkillVfxType → 범용 AOE 스프라이트 매핑 (18개 전부 커버)
private val AoeVfxMap: Map<SkillVfxType, String> = mapOf(
    // Fire
    SkillVfxType.VOLCANIC_ERUPTION to "fx_aoe_fire",
    SkillVfxType.FIRESTORM_METEOR to "fx_aoe_meteor",
    SkillVfxType.LINGERING_FLAME to "fx_penetrate",
    // Frost
    SkillVfxType.ICE_AGE_BLIZZARD to "fx_aoe_ice",
    SkillVfxType.ABSOLUTE_ZERO to "fx_aoe_ice",
    SkillVfxType.FROST_NOVA to "fx_splash",
    // Poison
    SkillVfxType.TOXIC_DOMAIN to "fx_aoe_dark",
    SkillVfxType.ACID_SPRAY to "fx_aoe_dark",
    SkillVfxType.POISON_CLOUD to "fx_penetrate",
    // Lightning
    SkillVfxType.THUNDERSTORM to "fx_aoe_lightning",
    SkillVfxType.STATIC_FIELD to "fx_aoe_lightning",
    SkillVfxType.LIGHTNING_STRIKE to "fx_chain_lightning",
    // Support
    SkillVfxType.DIVINE_SHIELD to "fx_aoe_holy",
    SkillVfxType.WAR_SONG_AURA to "fx_buff_attack",
    SkillVfxType.HEAL_PULSE to "fx_buff_heal",
    // Wind
    SkillVfxType.VACUUM_SLASH to "fx_aoe_earth",
    SkillVfxType.EYE_OF_STORM to "fx_aoe_earth",
    SkillVfxType.CYCLONE_PULL to "fx_splash",
)

// blueprintId → 궁극기 전용 스프라이트 (AoeVfxMap보다 우선)
private val UltSpriteMap: Map<String, String> = mapOf(
    // LEGEND
    "human_legend_01"  to "fx_ult_sword",
    "human_legend_02"  to "fx_ult_wisdom",
    "human_legend_03"  to "fx_ult_divine",
    "spirit_legend_01" to "fx_ult_cataclysm",
    "spirit_legend_02" to "fx_ult_nature",
    "spirit_legend_03" to "fx_ult_abyss",
    "animal_legend_01" to "fx_ult_dragon",
    "animal_legend_02" to "fx_ult_glacier",
    "animal_legend_03" to "fx_ult_tiger",
    "robot_legend_01"  to "fx_ult_orbital",
    "robot_legend_02"  to "fx_ult_emp",
    "robot_legend_03"  to "fx_ult_titan",
    "demon_legend_01"  to "fx_ult_hellfire",
    "demon_legend_02"  to "fx_ult_reaper",
    "demon_legend_03"  to "fx_ult_apocalypse",
    // MYTHIC
    "human_mythic_01"  to "fx_ult_divine_2",
    "human_mythic_02"  to "fx_ult_judgement",
    "human_mythic_03"  to "fx_ult_genesis",
    "spirit_mythic_01" to "fx_ult_elemental",
    "spirit_mythic_02" to "fx_ult_eternal",
    "spirit_mythic_03" to "fx_ult_endless",
    "animal_mythic_01" to "fx_ult_four",
    "animal_mythic_02" to "fx_ult_holyfire",
    "animal_mythic_03" to "fx_ult_justice",
    "robot_mythic_01"  to "fx_ult_quantum",
    "robot_mythic_02"  to "fx_ult_absolute",
    "robot_mythic_03"  to "fx_ult_emstorm",
    "demon_mythic_01"  to "fx_ult_dark",
    "demon_mythic_02"  to "fx_ult_lucifer",
    "demon_mythic_03"  to "fx_ult_doomsday",
    // IMMORTAL
    "human_immortal_01"  to "fx_ult_god",
    "spirit_immortal_01" to "fx_ult_world",
    "animal_immortal_01" to "fx_ult_divine_3",
    "robot_immortal_01"  to "fx_ult_nexus",
    "demon_immortal_01"  to "fx_ult_chaos",
)

/** DrawScope wrapper — size를 필드 크기로 변경하여 정규화 좌표 사용 */
private class FieldSizeDrawScope(
    private val delegate: DrawScope,
    override val size: Size,
) : DrawScope by delegate

@Composable
fun SkillEffectOverlay(
    fieldOffset: Offset = Offset.Zero,
    fieldSize: Size = Size.Zero,
) {
    val skillEvents by BattleBridge.skillEvents.collectAsState()
    val context = LocalContext.current

    val spriteBitmaps: Map<String, ImageBitmap?> = remember {
        (AoeVfxMap.values.toSet() + UltSpriteMap.values.toSet())
            .associateWith { name -> decodeAssetBitmap(context, "fx/$name.png") }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500L)
            BattleBridge.clearExpiredSkillEvents()
        }
    }

    if (skillEvents.isEmpty() || fieldSize == Size.Zero) return

    val eventsSnapshot = skillEvents

    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = System.currentTimeMillis()
        drawContext.canvas.save()
        drawContext.canvas.translate(fieldOffset.x, fieldOffset.y)
        val fieldDraw = FieldSizeDrawScope(this, fieldSize)
        with(fieldDraw) {
            for (i in eventsSnapshot.indices) {
                val event = eventsSnapshot.getOrNull(i) ?: continue
                val elapsed = (now - event.startTime) / 1000f
                if (event.duration <= 0f) continue
                val progress = (elapsed / event.duration).coerceIn(0f, 1f)

                // 궁극기 전용 스프라이트 우선, 없으면 범용 AOE 스프라이트
                val ultAsset = UltSpriteMap[event.abilityId]
                val asset = ultAsset ?: AoeVfxMap[event.type] ?: continue
                val bmp = spriteBitmaps[asset] ?: continue

                val cx = event.x * size.width
                val cy = event.y * size.height
                val radius = event.radius * size.minDimension
                val spriteR = if (ultAsset != null) radius * 1.2f else radius * 1.0f
                val fadeAlpha = when {
                    progress < 0.1f -> progress / 0.1f
                    progress > 0.9f -> (1f - progress) / 0.1f
                    else -> 1f
                }.coerceIn(0f, 1f)
                val pulse = 1f + sin(elapsed * 3f) * 0.05f
                val s = (spriteR * 2f * pulse).toInt().coerceAtLeast(1)
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset((cx - s / 2f).toInt(), (cy - s / 2f).toInt()),
                    dstSize = IntSize(s, s),
                    alpha = fadeAlpha * 0.85f,
                    blendMode = BlendMode.Screen,
                )
            }
        }
        drawContext.canvas.restore()
    }
}
