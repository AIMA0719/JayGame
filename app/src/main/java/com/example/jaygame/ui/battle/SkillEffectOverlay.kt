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
    "spirit_legend_03" to "fx_skill_abyss_call",       // 어둠현자 궁
    "animal_legend_01" to "fx_ult_dragon",
    "animal_legend_02" to "fx_skill_xuanwu_ultimate",   // 현무 궁
    "animal_legend_03" to "fx_skill_tiger_ultimate",    // 백호 궁
    "robot_legend_01"  to "fx_ult_orbital",
    "robot_legend_02"  to "fx_ult_emp",
    "robot_legend_03"  to "fx_skill_titan_smash",       // 타이탄 궁
    "demon_legend_01"  to "fx_skill_purgatory",         // 아크데몬 궁
    "demon_legend_02"  to "fx_skill_soul_harvest",      // 데스로드 궁
    "demon_legend_03"  to "fx_skill_doomsday",          // 아자젤 궁
    // MYTHIC
    "human_mythic_01"  to "fx_ult_divine_2",
    "human_mythic_02"  to "fx_ult_judgement",
    "human_mythic_03"  to "fx_ult_genesis",
    "spirit_mythic_01" to "fx_ult_elemental",
    "spirit_mythic_02" to "fx_ult_eternal",
    "spirit_mythic_03" to "fx_ult_endless",
    "animal_mythic_01" to "fx_ult_four",
    "animal_mythic_02" to "fx_skill_celestial_grace",   // 기린 궁
    "animal_mythic_03" to "fx_skill_judge_ultimate",    // 해태 궁
    "robot_mythic_01"  to "fx_ult_quantum",
    "robot_mythic_02"  to "fx_ult_absolute",
    "robot_mythic_03"  to "fx_skill_extermination",     // 네메시스 궁
    "demon_mythic_01"  to "fx_ult_dark",
    "demon_mythic_02"  to "fx_skill_paradise_lost",     // 루시퍼 궁
    "demon_mythic_03"  to "fx_skill_abyss_king",        // 아바돈 궁
    // IMMORTAL
    "human_immortal_01"  to "fx_ult_god",
    "spirit_immortal_01" to "fx_ult_world",
    "animal_immortal_01" to "fx_ult_divine_3",
    "robot_immortal_01"  to "fx_skill_digital_apocalypse", // 넥서스 궁
    "demon_immortal_01"  to "fx_skill_world_end",          // 혼돈의군주 궁
    // SPECIAL
    "special_dragon_king"      to "fx_skill_xuanwu_ultimate", // 용왕 궁 (폭풍해일 - 물 계열)
    "special_time_keeper"      to "fx_skill_flash",           // 시간의관리자 궁 (영겁의시간)
    "special_void_emperor"     to "fx_skill_dark_veil",       // 공허의황제 궁 (허무의파동)
    "special_infernal_titan"   to "fx_skill_hellfire",        // 지옥의거신 궁 (자폭)
    "special_primordial_chaos" to "fx_skill_elemental_harmony", // 원초의혼돈 궁 (만물귀환)
    "special_shadow_monarch"   to "fx_skill_nightmare",       // 그림자군주 궁 (그림자참수)
    "special_frost_emperor"    to "fx_skill_xuanwu_barrier",  // 빙제 궁 (빙하시대)
)

// abilityId → 패시브 스킬 스프라이트 (ability 발동 시 표시)
private val PassiveVfxMap: Map<String, String> = mapOf(
    // ── 정령 (SPIRIT) ──
    "earth_spirit_quake"     to "fx_skill_earth_power",      // 토정령 - 대지의힘
    "flash_burst"            to "fx_skill_flash",             // 빛정령 - 섬광
    "forest_root"            to "fx_skill_root_bind",         // 숲의정령 - 뿌리묶기
    "flame_burn"             to "fx_skill_burn",              // 화염술사 - 연소
    "elemental_harmony"      to "fx_skill_elemental_harmony", // 정령왕 - 원소조화
    "dark_veil"              to "fx_skill_dark_veil",         // 어둠현자 - 어둠의장막
    // ── 동물 (ANIMAL) ──
    "agility"                to "fx_skill_agility",           // 여우 - 민첩
    "hard_shell"             to "fx_skill_hard_shell",        // 거북 - 단단한등껍질
    "venom_sting"            to "fx_skill_venom_sting",       // 전갈 - 맹독침
    "dive_attack"            to "fx_skill_dive_attack",       // 매 - 급강하
    "petrify_gaze"           to "fx_skill_petrify_gaze",     // 바실리스크 - 석화시선
    "sky_dive"               to "fx_skill_sky_dive",          // 그리폰 - 천공강하
    "xuanwu_barrier"         to "fx_skill_xuanwu_barrier",    // 현무 - 현무의방벽
    "tiger_claw"             to "fx_skill_tiger_claw",        // 백호 - 맹호출산
    "sacred_flame"           to "fx_skill_sacred_flame",      // 기린 - 성스러운불꽃
    "justice_flame"           to "fx_skill_justice_flame",     // 해태 - 정의의불꽃
    // ── 로봇 (ROBOT) ──
    "repair_nano"            to "fx_skill_repair_nano",       // 수리봇 - 수리나노
    "self_destruct"          to "fx_skill_self_destruct",     // 지뢰봇 - 자폭프로토콜
    "scout_marking"          to "fx_skill_marking",           // 스카우트 - 마킹
    "energy_shield"          to "fx_skill_energy_shield",     // 실드봇 - 에너지실드
    "laser_pierce"           to "fx_skill_laser_pierce",      // 레이저봇 - 관통레이저
    "plasma_cannon"          to "fx_skill_plasma_cannon",     // 플라즈마봇 - 플라즈마포
    "energy_charge"          to "fx_skill_energy_charge",     // 퓨전코어 - 에너지차징
    "iron_wall"              to "fx_skill_iron_wall",         // 타이탄 - 철벽
    "emp_field"              to "fx_skill_emp_field",         // 네메시스 - 전자기장
    "quantum_network"        to "fx_skill_quantum_network",   // 넥서스 - 양자네트워크
    // ── 악마 (DEMON) ──
    "petrify"                to "fx_skill_petrify",           // 가고일 - 석화
    "curse_wave"             to "fx_skill_curse_wave",        // 원령 - 저주파동
    "poison_arrow"           to "fx_skill_poison_arrow",      // 다크엘프 - 독화살
    "nightmare"              to "fx_skill_nightmare",         // 나이트메어 - 악몽
    "blood_magic"            to "fx_skill_blood_magic",       // 블러드메이지 - 혈액마법
    "necromancy"             to "fx_skill_necromancy",        // 리치 - 사령술
    "blood_drain"            to "fx_skill_blood_drain",       // 뱀파이어 - 흡혈
    "death_aura"             to "fx_skill_death_aura",        // 데스나이트 - 죽음의오라
    "hellfire"               to "fx_skill_hellfire",          // 아크데몬 - 지옥의화염
    "death_fog"              to "fx_skill_death_fog",         // 데스로드 - 죽음의안개
    "corruption"             to "fx_skill_corruption",        // 아자젤 - 타락의속삭임
    "fallen_angel_light"     to "fx_skill_fallen_light",      // 루시퍼 - 타천사의빛
    "destruction_breath"     to "fx_skill_void_breath",       // 아바돈 - 파멸의숨결
    "chaos_control"          to "fx_skill_chaos_control",     // 혼돈의군주 - 혼돈의지배
    // ── 스페셜 (SPECIAL) ──
    "dragon_king_dominion"   to "fx_skill_sacred_flame",      // 용왕 - 용왕의위엄
    "time_keeper_slow"       to "fx_skill_flash",             // 시간의관리자 - 시간왜곡
    "void_emperor_nullify"   to "fx_skill_dark_veil",         // 공허의황제 - 공허잠식
    "infernal_titan_burn"    to "fx_skill_hellfire",          // 지옥의거신 - 지옥의화로
    "primordial_chaos_shift" to "fx_skill_elemental_harmony", // 원초의혼돈 - 원소변환
    "shadow_monarch_stealth" to "fx_skill_nightmare",         // 그림자군주 - 그림자지배
    "frost_emperor_permafrost" to "fx_skill_xuanwu_barrier",  // 빙제 - 영구동토
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
        (AoeVfxMap.values.toSet() + UltSpriteMap.values.toSet() + PassiveVfxMap.values.toSet())
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

                // 궁극기 전용 > 패시브 전용 > 범용 AOE 스프라이트
                val ultAsset = UltSpriteMap[event.abilityId]
                val passiveAsset = PassiveVfxMap[event.abilityId]
                val asset = ultAsset ?: passiveAsset ?: AoeVfxMap[event.type] ?: continue
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
                    alpha = fadeAlpha,
                )
            }
        }
        drawContext.canvas.restore()
    }
}
