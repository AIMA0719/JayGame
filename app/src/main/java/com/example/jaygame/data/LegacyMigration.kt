package com.example.jaygame.data

/**
 * Migrates legacy save data from List<UnitProgress> (indexed by Int) to Map<String, UnitProgress>
 * keyed by blueprintId.
 *
 * Legacy UNIT_DEFS had 42 indices (0-41) across 6 families x 7 grades.
 * New blueprints.json uses string IDs. Only units that exist in blueprints.json are migrated;
 * legacy high-tier units (MYTHIC/IMMORTAL) that have no blueprint counterpart are dropped.
 *
 * Mapping verified against:
 *   - UnitDefs.kt UNIT_DEFS (legacy index → family + grade)
 *   - blueprints.json (blueprintId → family + grade + role)
 */
object LegacyMigration {

    /**
     * Legacy Int index → new blueprintId.
     * Only entries that have a matching blueprint are included.
     * Indices with no match (MYTHIC/IMMORTAL tiers for frost/poison/lightning/support/wind)
     * are intentionally omitted — those units no longer exist in the new system.
     */
    private val LEGACY_INDEX_TO_BLUEPRINT: Map<Int, String> = mapOf(
        // ── FIRE (fire_mdps chain: 7 grades, COMMON → IMMORTAL) ──
        0  to "fire_mdps_01",   // 루비       COMMON
        5  to "fire_mdps_02",   // 카르마     RARE
        10 to "fire_mdps_03",   // 이그니스   HERO
        15 to "fire_mdps_04",   // 인페르노   LEGEND
        20 to "fire_mdps_05",   // 화산왕     ANCIENT
        25 to "fire_mdps_06",   // 피닉스     MYTHIC
        30 to "fire_mdps_07",   // 태양신 라  IMMORTAL

        // ── FROST (frost_rdps chain: 5 grades, COMMON → ANCIENT) ──
        1  to "frost_rdps_01",  // 미스트     COMMON
        6  to "frost_rdps_02",  // 프로스트   RARE
        11 to "frost_rdps_03",  // 블리자드   HERO
        16 to "frost_rdps_04",  // 아이스본   LEGEND
        21 to "frost_rdps_05",  // 빙하제왕   ANCIENT
        // 26 (유키 MYTHIC), 31 (크로노스 IMMORTAL) — no blueprint match

        // ── POISON (poison_mdps chain: 5 grades, COMMON → ANCIENT) ──
        2  to "poison_mdps_01", // 베놈       COMMON
        7  to "poison_mdps_02", // 바이퍼     RARE
        12 to "poison_mdps_03", // 플레이그   HERO
        17 to "poison_mdps_04", // 코로시브   LEGEND
        22 to "poison_mdps_05", // 헤카테     ANCIENT
        // 27 (니드호그 MYTHIC), 32 (아포칼립스 IMMORTAL) — no blueprint match

        // ── LIGHTNING (lightning_mdps chain: 5 grades, COMMON → ANCIENT) ──
        3  to "lightning_mdps_01", // 스파크   COMMON
        8  to "lightning_mdps_02", // 볼트     RARE
        13 to "lightning_mdps_03", // 썬더     HERO
        18 to "lightning_mdps_04", // 스톰     LEGEND
        23 to "lightning_mdps_05", // 뇌왕     ANCIENT
        // 28 (토르 MYTHIC), 33 (제우스 IMMORTAL) — no blueprint match

        // ── SUPPORT (support_support chain: 5 grades, COMMON → ANCIENT) ──
        4  to "support_support_01", // 뮤즈     COMMON
        9  to "support_support_02", // 가디언   RARE
        14 to "support_support_03", // 오라클   HERO
        19 to "support_support_04", // 발키리   LEGEND
        24 to "support_support_05", // 세라핌   ANCIENT
        // 29 (아르카나 MYTHIC), 34 (가이아 IMMORTAL) — no blueprint match

        // ── WIND (wind_mdps chain: 5 grades, COMMON → ANCIENT) ──
        35 to "wind_mdps_01",   // 제피르     COMMON
        36 to "wind_mdps_02",   // 게일       RARE
        37 to "wind_mdps_03",   // 사이클론   HERO
        38 to "wind_mdps_04",   // 태풍       LEGEND
        39 to "wind_mdps_05",   // 하늘군주   ANCIENT
        // 40 (실프 MYTHIC), 41 (바유 IMMORTAL) — no blueprint match
    )

    /**
     * Migrate a legacy List<UnitProgress> (index-based) to Map<String, UnitProgress> (blueprintId-keyed).
     * Only units with a valid blueprint mapping AND that have meaningful progress (owned or have cards)
     * are included. Default (un-owned, 0 cards, level 1) entries are omitted since the new system
     * returns a default UnitProgress for missing keys.
     */
    fun migrateUnits(legacyUnits: List<UnitProgress>): Map<String, UnitProgress> {
        val result = mutableMapOf<String, UnitProgress>()
        for ((legacyIndex, blueprintId) in LEGACY_INDEX_TO_BLUEPRINT) {
            if (legacyIndex in legacyUnits.indices) {
                val progress = legacyUnits[legacyIndex]
                // Always include if the player had any progress with this unit
                if (progress.owned || progress.cards > 0 || progress.level > 1) {
                    result[blueprintId] = progress
                }
            }
        }
        return result
    }

    /** Look up the blueprintId for a legacy index, or null if unmapped. */
    fun blueprintIdForLegacyIndex(index: Int): String? = LEGACY_INDEX_TO_BLUEPRINT[index]
}
