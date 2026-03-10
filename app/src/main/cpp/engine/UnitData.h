#ifndef JAYGAME_UNITDATA_H
#define JAYGAME_UNITDATA_H

#include <cstdlib>
#include <cstddef>
#include <cassert>

// --------------- Enums ---------------

enum class UnitRarity { Normal, Rare, Epic, Legendary };
enum class UnitElement { Physical, Magic, Support };
enum class AbilityType {
    None, Splash, Slow, DoT, Chain, Buff,
    Debuff, Summon, Shield, Execute, SPBonus
};

// --------------- Static unit definition ---------------

struct UnitDef {
    int id;
    const char* name;
    UnitRarity rarity;
    UnitElement element;
    float baseATK;        // damage per hit
    float atkSpeed;       // attacks per second
    float range;          // in logical pixels
    AbilityType ability;
    float abilityValue;   // ability-specific parameter
};

// --------------- Unit table (10 units) ---------------
//
// Normal (4):
//   0  화염   Fire      - Splash (AoE radius)
//   1  냉기   Frost     - Slow   (slow %)
//   2  독     Poison    - DoT    (DPS over time)
//   3  철벽   IronWall  - Shield (shield HP)
//
// Rare (3):
//   4  번개   Lightning - Chain  (bounce count)
//   5  저격   Sniper    - None   (high single-target)
//   6  강화   Enhance   - Buff   (ATK % bonus to neighbors)
//
// Epic (2):
//   7  폭풍   Storm     - Splash+Slow (radius; slow baked into ability)
//   8  암살   Assassin  - Execute (HP % threshold)
//
// Legendary (1):
//   9  용     Dragon    - Splash (massive AoE radius)

inline constexpr UnitDef UNIT_TABLE[] = {
    // id  name         rarity                element              ATK   spd   range   ability              value
    {  0, "화염",   UnitRarity::Normal,    UnitElement::Magic,     25.f, 1.0f, 200.f, AbilityType::Splash,  80.f  },
    {  1, "냉기",   UnitRarity::Normal,    UnitElement::Magic,     18.f, 1.2f, 180.f, AbilityType::Slow,     0.3f },
    {  2, "독",     UnitRarity::Normal,    UnitElement::Magic,     15.f, 0.8f, 160.f, AbilityType::DoT,     10.f  },
    {  3, "철벽",   UnitRarity::Normal,    UnitElement::Support,   10.f, 0.6f, 120.f, AbilityType::Shield,  50.f  },

    {  4, "번개",   UnitRarity::Rare,      UnitElement::Magic,     35.f, 0.9f, 220.f, AbilityType::Chain,    3.f  },
    {  5, "저격",   UnitRarity::Rare,      UnitElement::Physical,  60.f, 0.5f, 350.f, AbilityType::None,     0.f  },
    {  6, "강화",   UnitRarity::Rare,      UnitElement::Support,   12.f, 1.0f, 150.f, AbilityType::Buff,     0.2f },

    {  7, "폭풍",   UnitRarity::Epic,      UnitElement::Magic,     45.f, 0.7f, 250.f, AbilityType::Splash, 100.f  },
    {  8, "암살",   UnitRarity::Epic,      UnitElement::Physical,  80.f, 0.4f, 200.f, AbilityType::Execute,  0.15f},

    {  9, "용",     UnitRarity::Legendary, UnitElement::Magic,    100.f, 0.3f, 300.f, AbilityType::Splash, 140.f  },

    // Hidden combination units (not available from summoning)
    // 10: 전기독 ElectroPoison (번개+독) - Chain + DoT
    { 10, "전기독", UnitRarity::Epic,      UnitElement::Magic,     40.f, 0.9f, 220.f, AbilityType::Chain,    4.f  },
    // 11: 처형자 Executioner (저격+암살) - Execute with huge multiplier
    { 11, "처형자", UnitRarity::Legendary, UnitElement::Physical,  90.f, 0.4f, 300.f, AbilityType::Execute,  0.3f },
    // 12: 성채 Citadel (강화+철벽) - Buff + Shield aura
    { 12, "성채",   UnitRarity::Epic,      UnitElement::Support,   15.f, 0.8f, 180.f, AbilityType::Buff,     0.3f },
    // 13: 불사조 Phoenix (화염+용) - Massive splash + DoT
    { 13, "불사조", UnitRarity::Legendary, UnitElement::Magic,    120.f, 0.25f, 280.f, AbilityType::Splash, 160.f },
    // 14: Reserved for future combination
    { 14, "영혼",   UnitRarity::Legendary, UnitElement::Magic,     70.f, 0.6f, 250.f, AbilityType::Chain,    5.f  },
};

inline constexpr size_t UNIT_TABLE_SIZE = sizeof(UNIT_TABLE) / sizeof(UNIT_TABLE[0]);

// IDs 0-9 are summonable, 10+ are hidden combination results
inline constexpr int SUMMONABLE_UNIT_COUNT = 10;

// --------------- Summoning probabilities ---------------
// Normal 60%, Rare 25%, Epic 12%, Legendary 3%

inline const UnitDef& getUnitDef(int id) {
    assert(id >= 0 && id < static_cast<int>(UNIT_TABLE_SIZE));
    return UNIT_TABLE[id];
}

// Given a deck of up to 5 unit IDs, pick one at random respecting rarity weights.
// deckIds: array of unit IDs in the player's deck
// deckSize: number of entries (1..5)
// Returns a reference to the chosen UnitDef.
inline const UnitDef& rollRandomUnit(const int* deckIds, int deckSize) {
    assert(deckSize > 0 && deckSize <= 5);

    // Step 1: compute total weight across deck entries
    float totalWeight = 0.f;
    float weights[5];
    for (int i = 0; i < deckSize; i++) {
        const UnitDef& def = getUnitDef(deckIds[i]);
        float w = 0.f;
        switch (def.rarity) {
            case UnitRarity::Normal:    w = 60.f; break;
            case UnitRarity::Rare:      w = 25.f; break;
            case UnitRarity::Epic:      w = 12.f; break;
            case UnitRarity::Legendary: w =  3.f; break;
        }
        weights[i] = w;
        totalWeight += w;
    }

    // Step 2: roll
    float roll = (static_cast<float>(std::rand()) / static_cast<float>(RAND_MAX)) * totalWeight;
    float cumulative = 0.f;
    for (int i = 0; i < deckSize; i++) {
        cumulative += weights[i];
        if (roll <= cumulative) {
            return getUnitDef(deckIds[i]);
        }
    }
    // Fallback (floating point edge case)
    return getUnitDef(deckIds[deckSize - 1]);
}

#endif // JAYGAME_UNITDATA_H
