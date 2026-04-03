package com.jay.jaygame.engine

enum class BossModifier(val label: String, val description: String) {
    PHYSICAL_RESIST("강철 피부", "물리 피해 60% 감소"),
    MAGIC_RESIST("마법 장벽", "마법 피해 60% 감소"),
    CC_IMMUNE("불굴의 의지", "둔화/동결 면역"),
    DOT_IMMUNE("정화의 피", "지속 피해 면역"),
    RANGED_RESIST("반사 아우라", "원거리 피해 50% 감소"),
    REGENERATION("재생", "10초마다 체력 5% 회복"),
    SWIFT("질풍", "이동속도 2배"),
    COMMANDER("지휘관", "주변 몬스터 방어 +50%"),
    BERSERKER("광전사", "체력 50% 이하 시 공격속도 3배"),
    SPLITTER("분열", "체력 50% 이하 시 미니 보스 2마리 소환"),
    SHIELDED("보호막", "5초마다 모든 피해 무효화 보호막"),
    VAMPIRIC("흡혈", "공격 피해의 20% 체력 회복"),

    // ── 신규 기믹 (기본 풀) ──
    MIRROR("거울", "받은 데미지 15% 반사 → 가장 가까운 유닛 2초 공격 불능"),
    PHANTOM("환영", "3초마다 1초간 투명 (데미지 면역 + 타겟팅 불가)"),
    GRAVITY("중력장", "주변 250px 유닛 공격속도 30% 감소"),

    // ── 후반 전용 기믹 ──
    DUAL_MOD("이중 기믹", "기존 기믹 2개 동시 적용"),
    ADAPTIVE("적응형", "가장 많이 받은 데미지 타입 저항 +40%"),
    MINION_RUSH("호위대", "엘리트 5마리 호위 — 전멸 전 보스 타겟 불가"),
}

/** 기본 보스 기믹 풀 (초중반) — DUAL_MOD, ADAPTIVE, MINION_RUSH 제외 */
val BASE_MODIFIER_POOL = BossModifier.entries.filter {
    it !in setOf(BossModifier.DUAL_MOD, BossModifier.ADAPTIVE, BossModifier.MINION_RUSH)
}

/** 후반 전용 기믹 풀 (웨이브 40+) */
private val LATE_MODIFIER_POOL = listOf(
    BossModifier.DUAL_MOD, BossModifier.ADAPTIVE, BossModifier.MINION_RUSH,
)

fun getBossModifier(stageId: Int, waveIndex: Int): BossModifier? {
    if ((waveIndex + 1) % 10 != 0) return null
    val bossNumber = (waveIndex + 1) / 10

    // 후반(보스 5~6, 즉 웨이브 49/59): 후반 전용 기믹 랜덤
    if (bossNumber >= 5) return LATE_MODIFIER_POOL.random()

    return when (stageId) {
        0 -> when (bossNumber) {
            1 -> null; 2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.SWIFT; 4 -> BossModifier.REGENERATION; else -> null
        }
        1 -> when (bossNumber) {
            1 -> BossModifier.DOT_IMMUNE; 2 -> BossModifier.MAGIC_RESIST
            3 -> BossModifier.CC_IMMUNE; 4 -> BossModifier.COMMANDER; else -> null
        }
        2 -> when (bossNumber) {
            1 -> BossModifier.RANGED_RESIST; 2 -> BossModifier.PHYSICAL_RESIST
            3 -> BossModifier.REGENERATION; 4 -> BossModifier.MIRROR
            else -> BASE_MODIFIER_POOL.random()
        }
        3 -> when (bossNumber) {
            1 -> BossModifier.MAGIC_RESIST; 2 -> BossModifier.PHANTOM
            3 -> BossModifier.GRAVITY; 4 -> BossModifier.COMMANDER
            else -> BASE_MODIFIER_POOL.random()
        }
        4, 5 -> BASE_MODIFIER_POOL.random()
        else -> null
    }
}
