package com.jay.jaygame.ui.battle

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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jay.jaygame.bridge.BattleBridge
import com.jay.jaygame.bridge.SkillVfxType
import kotlin.math.sin

// AnimSheetInfo, loadAnimManifest → BitmapUtils.kt에 정의

// ─── 정적 스프라이트 이름 → 애니메이션 시트 매핑 ───
// HD=VFX Free Pack, SP=Super Pixel, FPE=Free Pixel Effects
// LT=Lightning Skills, FK=Frost Knight, EXP=Explosions, LP=LightPillar
// SFX=SlashFX, PIPOYA=Mysterious Object, RETRO=Retro Pixel Effects
private val AnimOverrideMap: Map<String, String> = mapOf(
    // ── 화염 계열 ──
    "fx_aoe_fire"          to "anim_hd_magma",            // 화산폭발 → HD 마그마
    "fx_aoe_meteor"        to "anim_exp_nuclear",          // 메테오 → EXP 핵폭발
    "fx_penetrate"         to "anim_hd_fastfire",          // 화염 관통 → HD 빠른불
    "fx_skill_hellfire"    to "anim_hd_fire",              // 지옥화염 → HD 디더링화염
    "fx_skill_burn"        to "anim_fpe_flamelash",        // 연소 → FPE 화염채찍
    "fx_skill_sacred_flame" to "anim_fpe_brightfire",      // 성스러운불꽃 → FPE 밝은불
    "fx_skill_justice_flame" to "anim_fpe_firespin",       // 정의불꽃 → FPE 화염회전
    "fx_skill_purgatory"   to "anim_exp_fire",             // 연옥 → EXP 화염폭발
    "fx_ult_sword"         to "anim_sfx3a_1",             // 검기 궁 → SFX 3A 백색검기(192px HD)
    "fx_ult_dragon"        to "anim_exp_two_colors",       // 드래곤 궁 → EXP 투컬러폭발
    "fx_skill_doomsday"    to "anim_exp_nuclear",          // 최후의날 → EXP 핵폭발
    "fx_skill_self_destruct" to "anim_sp_epic_exp_orange", // 자폭 → SP 에픽폭발
    "fx_dot_fire"          to "anim_fpe_fire",             // 화염도트 → FPE 화염

    // ── 얼음/물 계열 ──
    "fx_aoe_ice"           to "anim_frost_skill1",         // 빙결 범용 → FK 빙결1
    "fx_splash"            to "anim_exp_blue_circle",      // 스플래시 → EXP 파란원폭발
    "fx_freeze"            to "anim_frost_skill2",         // 빙결 → FK 빙결2
    "fx_skill_xuanwu_barrier" to "anim_frost_skill3",      // 현무방벽 → FK 빙결3
    "fx_skill_xuanwu_ultimate" to "anim_frost_skill1",     // 현무 궁 → FK 빙결1
    "fx_slow"              to "anim_exp_blue_oval",        // 슬로우 → EXP 파란타원폭발

    // ── 번개 계열 ──
    "fx_aoe_lightning"     to "anim_lightning_skill3",     // 번개 범용 → LT 수직번개
    "fx_chain_lightning"   to "anim_lightning_skill1",     // 체인라이트닝 → LT 수평번개
    "fx_skill_plasma_cannon" to "anim_lightning_skill6",   // 플라즈마 → LT 볼라이트닝
    "fx_skill_emp_field"   to "anim_lightning_skill4",     // EMP → LT 번개폭발
    "fx_skill_laser_pierce" to "anim_lightning_skill2",    // 레이저 → LT 번개빔
    "fx_stun"              to "anim_lightning_skill5",     // 스턴 → LT 번개임팩트

    // ── 독/어둠 계열 ──
    "fx_aoe_dark"          to "anim_fpe_midnight",         // 암흑 AOE → FPE 미드나이트
    "fx_dot_poison"        to "anim_exp_gas",              // 독도트 → EXP 가스
    "fx_skill_dark_veil"   to "anim_fpe_phantom",          // 어둠장막 → FPE 팬텀
    "fx_skill_corruption"  to "anim_hd_worm",              // 타락 → HD 웜
    "fx_skill_curse_wave"  to "anim_fpe_felspell",         // 저주파동 → FPE 저주마법
    "fx_skill_nightmare"   to "anim_hd_tentacles",         // 악몽 → HD 촉수
    "fx_skill_death_aura"  to "anim_fpe_nebula",           // 죽음오라 → FPE 네뷸라
    "fx_skill_death_fog"   to "anim_exp_gas_circle",       // 죽음안개 → EXP 가스원폭발
    "fx_skill_necromancy"  to "anim_sp_death",             // 사령술 → SP 죽음
    "fx_skill_soul_harvest" to "anim_sp_absorb",           // 영혼수확 → SP 흡수
    "fx_skill_blood_magic" to "anim_sfx3a_2",             // 혈액마법 → SFX 3A 빨강검기
    "fx_skill_blood_drain" to "anim_spm_fx1_splatter_small_red", // 흡혈 → SPM 피스플래터
    "fx_skill_poison_arrow" to "anim_sp_poison",           // 독화살 → SP 독
    "fx_skill_venom_sting" to "anim_exp_gas",              // 맹독침 → EXP 가스
    "fx_skill_petrify"     to "anim_sp_impact_violet",     // 석화 → SP 임팩트보라
    "fx_skill_petrify_gaze" to "anim_spm_fx2_impact_shock_large_brown", // 석화시선 → SPM 충격
    "fx_skill_void_breath" to "anim_sfx3b_4",             // 파멸숨결 → SFX 3B 녹색콤보슬래시
    "fx_skill_abyss_call"  to "anim_pipoya_pipo-mapeffect021_480", // 심연부름 → PIPOYA 신비1
    "fx_skill_abyss_king"  to "anim_pipoya_pipo-mapeffect022_480", // 심연왕 → PIPOYA 신비2
    "fx_skill_paradise_lost" to "anim_hd_constellation",   // 실낙원 → HD 별자리
    "fx_skill_fallen_light" to "anim_lp_pipo-mapeffect013a", // 타천사빛 → LP 빛기둥A
    "fx_ult_dark"          to "anim_pipoya_pipo-mapeffect023_480", // 암흑 궁 → PIPOYA 신비3
    "fx_ult_endless"       to "anim_hd_vortex",            // 무한 궁 → HD 소용돌이

    // ── 성스러운/서포트 계열 ──
    "fx_aoe_holy"          to "anim_lp_pipo-mapeffect013b", // 신성 AOE → LP 빛기둥B
    "fx_buff_attack"       to "anim_sp_atkup",             // 공격버프 → SP ATK업
    "fx_buff_heal"         to "anim_sp_heal",              // 힐 → SP 힐
    "fx_buff_defense"      to "anim_sp_defup",             // 방어버프 → SP DEF업
    "fx_buff_shield"       to "anim_hd_electric",          // 쉴드 → HD 전기실드
    "fx_buff_speed"        to "anim_sp_haste",             // 속도버프 → SP 헤이스트
    "fx_buff_powerup"      to "anim_hd_charged",           // 파워업 → HD 차징
    "fx_skill_repair_nano" to "anim_sp_heal",              // 수리나노 → SP 힐
    "fx_skill_energy_shield" to "anim_spm_fx2_electric_burst_large_violet", // 에너지실드
    "fx_skill_energy_charge" to "anim_hd_charged",         // 에너지차징 → HD 차징
    "fx_skill_iron_wall"   to "anim_frost_skill3",         // 철벽 → FK 얼음방벽
    "fx_skill_celestial_grace" to "anim_hd_anima",         // 천상은총 → HD 애니마
    "fx_skill_elemental_harmony" to "anim_pipoya_pipo-mapeffect025_480", // 원소조화 → PIPOYA 신비5
    "fx_ult_divine"        to "anim_lp_pipo-mapeffect013c", // 신성 궁 → LP 빛기둥C
    "fx_ult_divine_2"      to "anim_hd_eldenring",         // 신성2 궁 → HD 엘든링
    "fx_ult_divine_3"      to "anim_hd_constellation",     // 신성3 궁 → HD 별자리
    "fx_ult_god"           to "anim_lp_pipo-mapeffect013a-front", // 신 궁 → LP 빛기둥A앞
    "fx_ult_wisdom"        to "anim_fpe_magic8",           // 지혜 궁 → FPE 매직8
    "fx_ult_nature"        to "anim_sp_sparkle_green",     // 자연 궁 → SP 초록스파클
    "fx_ult_judgement"     to "anim_hd_explosion",         // 심판 궁 → HD 폭발
    "fx_ult_genesis"       to "anim_pipoya_pipo-mapeffect024_480", // 창세 궁 → PIPOYA 신비4
    "fx_skill_judge_ultimate" to "anim_fpe_sunburn",       // 해태 궁 → FPE 선번

    // ── 바람/대지 계열 ──
    "fx_aoe_earth"         to "anim_hd_hyperspeed",        // 대지/바람 → HD 초고속
    "fx_skill_earth_power" to "anim_exp_circle",           // 대지의힘 → EXP 원형폭발
    "fx_skill_root_bind"   to "anim_sp_sparkle_green",     // 뿌리묶기 → SP 초록스파클
    "fx_skill_dive_attack" to "anim_sfx5a_v1_1",          // 급강하 → SFX 5A 백색슬래시
    "fx_skill_sky_dive"    to "anim_sfx5a_v2_1",          // 천공강하 → SFX 5A v2 백색슬래시
    "fx_skill_tiger_claw"  to "anim_sfx3d_1",             // 맹호출산 → SFX 3D 백색할퀴기
    "fx_skill_tiger_ultimate" to "anim_sfx3b_1",          // 백호 궁 → SFX 3B 백색콤보
    "fx_skill_titan_smash" to "anim_sp_epic_exp_yellow",   // 타이탄 궁 → SP 에픽폭발노랑

    // ── 특수/기술 계열 ──
    "fx_skill_flash"       to "anim_sp_light_burst",       // 섬광 → SP 라이트버스트
    "fx_skill_marking"     to "anim_sp_sparkle_blue",      // 마킹 → SP 파랑스파클
    "fx_skill_agility"     to "anim_sfx5a_v3_1",          // 민첩 → SFX 5A v3 백색(빠른 느낌)
    "fx_skill_hard_shell"  to "anim_sp_defup",             // 등껍질 → SP 방어업
    "fx_skill_quantum_network" to "anim_pipoya_pipo-nazoobj01a_480", // 양자네트워크 → PIPOYA 오브1a
    "fx_skill_digital_apocalypse" to "anim_hd_eldenring",  // 디지털묵시록 → HD 엘든링
    "fx_skill_chaos_control" to "anim_pipoya_pipo-nazoobj02a_480", // 혼돈지배 → PIPOYA 오브2a
    "fx_skill_world_end"   to "anim_exp_nuclear",          // 세계종말 → EXP 핵폭발
    "fx_skill_extermination" to "anim_sfx3e_2",            // 섬멸 → SFX 3E 빨강 다중슬래시
    "fx_ult_orbital"       to "anim_hd_wheel",             // 궤도 궁 → HD 휠
    "fx_ult_emp"           to "anim_lightning_skill4",     // EMP 궁 → LT 번개폭발
    "fx_ult_quantum"       to "anim_pipoya_pipo-nazoobj03a_480", // 양자 궁 → PIPOYA 오브3a
    "fx_ult_absolute"      to "anim_frost_skill1",         // 절대 궁 → FK 빙결1
    "fx_ult_world"         to "anim_lp_pipo-mapeffect013b-front", // 세계 궁 → LP 빛기둥B앞
    "fx_ult_four"          to "anim_fpe_vortex",           // 사수 궁 → FPE 보텍스
    "fx_ult_elemental"     to "anim_pipoya_pipo-mapeffect025_480", // 원소 궁 → PIPOYA 신비5
    "fx_ult_eternal"       to "anim_lp_pipo-mapeffect013c-front", // 영원 궁 → LP 빛기둥C앞
    "fx_ult_cataclysm"     to "anim_sfx3b_3",             // 대재앙 궁 → SFX 3B 파란콤보슬래시
    "fx_crit"              to "anim_sfx2a_1",             // 크리티컬 → SFX 2A 백색슬래시
    "fx_execute"           to "anim_sfx3d_2",             // 처형 → SFX 3D 빨강할퀴기
    "fx_armor_break"       to "anim_sfx3e_1",             // 방어구파괴 → SFX 3E 백색 다중슬래시
)

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

// ─── 애니메이션 프레임 속도 (초/프레임) ───
private const val ANIM_FRAME_DURATION = 0.07f  // ~14fps

/** DrawScope wrapper — size를 필드 크기로 변경하여 정규화 좌표 사용 */
private class FieldSizeDrawScope(
    private val delegate: DrawScope,
    override val size: Size,
) : DrawScope by delegate

/** LRU 캐시 — 최대 20개 시트만 메모리에 유지 (시트당 ~0.7MB, 총 ~14MB 상한) */
private const val ANIM_CACHE_MAX = 20

@Composable
fun SkillEffectOverlay(
    fieldOffset: Offset = Offset.Zero,
    fieldSize: Size = Size.Zero,
) {
    val skillEvents by BattleBridge.skillEvents.collectAsState()
    val context = LocalContext.current

    val animManifest: Map<String, AnimSheetInfo> = remember { loadAnimManifest(context) }

    // LRU 캐시: 백그라운드에서 프리로드, 최대 ANIM_CACHE_MAX개
    val animCache = remember {
        object : LinkedHashMap<String, ImageBitmap?>(ANIM_CACHE_MAX + 2, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>): Boolean =
                size > ANIM_CACHE_MAX
        }
    }

    // 백그라운드에서 자주 쓰이는 시트 프리로드 (Canvas에서 decode 방지)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (name in AnimOverrideMap.values.toSet()) {
                if (animCache.size >= ANIM_CACHE_MAX) break
                if (name !in animCache) {
                    decodeAssetBitmap(context, "fx_anim/$name.png")?.let { animCache[name] = it }
                }
            }
        }
    }

    // 정적 스프라이트 — 애니메이션이 없는 것만 로드
    val animatedStaticNames = remember { AnimOverrideMap.keys }
    val spriteBitmaps: Map<String, ImageBitmap?> = remember {
        (AoeVfxMap.values.toSet() + UltSpriteMap.values.toSet() + PassiveVfxMap.values.toSet())
            .filter { it !in animatedStaticNames }
            .associateWith { name -> decodeAssetBitmap(context, "fx/$name.png") }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
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

                // 궁극기 전용 > 패시브 전용 > 범용 AOE 스프라이트 이름 결정
                val ultAsset = UltSpriteMap[event.abilityId]
                val passiveAsset = PassiveVfxMap[event.abilityId]
                val staticAsset = ultAsset ?: passiveAsset ?: AoeVfxMap[event.type] ?: continue

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
                val dstOffset = IntOffset((cx - s / 2f).toInt(), (cy - s / 2f).toInt())
                val dstSize = IntSize(s, s)

                // 애니메이션 시트 우선 → 없으면 정적 fallback
                val animName = AnimOverrideMap[staticAsset]
                val animInfo = if (animName != null) animManifest[animName] else null
                val animBmp = if (animName != null) animCache[animName] else null

                if (animInfo != null && animBmp != null && animInfo.frames > 1) {
                    // ── 스프라이트 시트 프레임 애니메이션 렌더링 ──
                    val frameIndex = ((elapsed / ANIM_FRAME_DURATION).toInt() % animInfo.frames)
                        .coerceIn(0, animInfo.frames - 1)
                    val srcX = frameIndex * animInfo.cellW
                    drawImage(
                        image = animBmp,
                        srcOffset = IntOffset(srcX, 0),
                        srcSize = IntSize(animInfo.cellW, animInfo.cellH),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = fadeAlpha,
                    )
                } else {
                    // ── 기존 정적 스프라이트 fallback ──
                    val bmp = spriteBitmaps[staticAsset] ?: continue
                    drawImage(
                        image = bmp,
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = fadeAlpha,
                    )
                }
            }
        }
        drawContext.canvas.restore()
    }
}
